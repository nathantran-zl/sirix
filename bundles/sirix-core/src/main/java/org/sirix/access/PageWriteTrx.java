/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sirix.access;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Optional;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.sirix.api.IPageWriteTrx;
import org.sirix.cache.ICache;
import org.sirix.cache.NodePageContainer;
import org.sirix.cache.TransactionLogCache;
import org.sirix.exception.AbsTTException;
import org.sirix.exception.TTIOException;
import org.sirix.io.IWriter;
import org.sirix.node.DeletedNode;
import org.sirix.node.ENode;
import org.sirix.node.delegates.NodeDelegate;
import org.sirix.node.interfaces.INode;
import org.sirix.page.IndirectPage;
import org.sirix.page.NamePage;
import org.sirix.page.NodePage;
import org.sirix.page.PageReference;
import org.sirix.page.RevisionRootPage;
import org.sirix.page.UberPage;
import org.sirix.page.interfaces.IPage;
import org.sirix.settings.ERevisioning;
import org.sirix.utils.IConstants;
import org.sirix.utils.NamePageHash;

/**
 * <h1>PageWriteTrx</h1>
 * 
 * <p>
 * Implements the {@link IPageWriteTrx} interface to provide write capabilities to the persistent storage
 * layer.
 * </p>
 */
public final class PageWriteTrx implements IPageWriteTrx {

  /** Page writer to serialize. */
  private final IWriter mPageWriter;

  /** Cache to store the changes in this writetransaction. */
  private final ICache<Long, NodePageContainer> mLog;

  /** Last references to the Nodepage, needed for pre/postcondition check. */
  private NodePageContainer mNodePageCon;

  /** Last reference to the actual revRoot. */
  private final RevisionRootPage mNewRoot;

  /** ID for current transaction. */
  private final long mTransactionID;

  /** {@link PageReadTrx} instance. */
  private final PageReadTrx mPageRtx;

  /**
   * Standard constructor.
   * 
   * @param pSession
   *          {@link ISessionConfiguration} this page write trx is bound to
   * @param pUberPage
   *          root of revision
   * @param pWriter
   *          writer where this transaction should write to
   * @param pId
   *          ID
   * @param pRepresentRev
   *          revision represent
   * @param pStoreRev
   *          revision store
   * @throws TTIOException
   *           if IO Error
   */
  PageWriteTrx(final Session pSession, final UberPage pUberPage, final IWriter pWriter, final long pId,
    final long pRepresentRev, final long pStoreRev, final long pLastCommitedRev) throws TTIOException {
    mPageRtx = new PageReadTrx(pSession, pUberPage, pRepresentRev, pWriter);
    final RevisionRootPage lastCommitedRoot =
      preparePreviousRevisionRootPage(pRepresentRev, pLastCommitedRev);
    mNewRoot = preparePreviousRevisionRootPage(pRepresentRev, pStoreRev);
    mNewRoot.setMaxNodeKey(lastCommitedRoot.getMaxNodeKey());
    mLog = new TransactionLogCache(this, pSession.mResourceConfig.mPath, pStoreRev);
    mPageWriter = pWriter;
    mTransactionID = pId;
  }

  @Override
  public INode prepareNodeForModification(final long pNodeKey) throws TTIOException {
    if (pNodeKey < 0) {
      throw new IllegalArgumentException("pNodeKey must be >= 0!");
    }
    if (mNodePageCon != null) {
      throw new IllegalStateException("Another node page container is currently in the cache for updates!");
    }

    final long nodePageKey = mPageRtx.nodePageKey(pNodeKey);
    final int nodePageOffset = mPageRtx.nodePageOffset(pNodeKey);
    prepareNodePage(nodePageKey);

    INode node = mNodePageCon.getModified().getNode(nodePageOffset);
    if (node == null) {
      final INode oldNode = mNodePageCon.getComplete().getNode(nodePageOffset);
      if (oldNode == null) {
        throw new TTIOException("Cannot retrieve node from cache");
      }
      node = oldNode;
      mNodePageCon.getModified().setNode(nodePageOffset, node);
    }

    return node;
  }

  @Override
  public <T extends INode> void finishNodeModification(@Nonnull final T pNode) {
    final long nodePageKey = mPageRtx.nodePageKey(pNode.getNodeKey());
    if (mNodePageCon == null || pNode == null || mLog.get(nodePageKey) == null) {
      throw new IllegalStateException();
    }

    mLog.put(nodePageKey, mNodePageCon);
    mNodePageCon = null;
  }

  @Override
  public <T extends INode> T createNode(@Nonnull final T pNode) throws TTIOException {
    // Allocate node key and increment node count.
    mNewRoot.incrementMaxNodeKey();
    final long nodeKey = mNewRoot.getMaxNodeKey();
    final long nodePageKey = mPageRtx.nodePageKey(nodeKey);
    final int nodePageOffset = mPageRtx.nodePageOffset(nodeKey);
    prepareNodePage(nodePageKey);
    final NodePage page = mNodePageCon.getModified();
    page.setNode(nodePageOffset, pNode);
    finishNodeModification(pNode);
    return pNode;
  }

  @Override
  public void removeNode(@Nonnull final INode pNode) throws TTIOException {
    final long nodePageKey = mPageRtx.nodePageKey(pNode.getNodeKey());
    prepareNodePage(nodePageKey);
    final INode delNode =
      new DeletedNode(new NodeDelegate(pNode.getNodeKey(), pNode.getParentKey(), pNode.getHash()));
    mNodePageCon.getModified().setNode(mPageRtx.nodePageOffset(pNode.getNodeKey()), delNode);
    mNodePageCon.getComplete().setNode(mPageRtx.nodePageOffset(pNode.getNodeKey()), delNode);
    finishNodeModification(pNode);
  }

  @Override
  public Optional<INode> getNode(@Nonnegative final long pNodeKey) throws TTIOException {
    // Calculate page and node part for given nodeKey.
    final long nodePageKey = mPageRtx.nodePageKey(pNodeKey);
    final int nodePageOffset = mPageRtx.nodePageOffset(pNodeKey);

    final NodePageContainer pageCont = mLog.get(nodePageKey);
    if (pageCont == null) {
      return mPageRtx.getNode(pNodeKey);
    } else if (pageCont.getModified().getNode(nodePageOffset) == null) {
      final INode item = pageCont.getComplete().getNode(nodePageOffset);
      return Optional.fromNullable(mPageRtx.checkItemIfDeleted(item));
    } else {
      final INode item = pageCont.getModified().getNode(nodePageOffset);
      return Optional.fromNullable(mPageRtx.checkItemIfDeleted(item));
    }
  }

  @Override
  public String getName(final int pNameKey, final ENode pNodeKind) {
    final NamePage currentNamePage = (NamePage)mNewRoot.getNamePageReference().getPage();
    // if currentNamePage == null -> state was commited and no prepareNodepage was invoked yet
    return (currentNamePage == null || currentNamePage.getName(pNameKey, pNodeKind) == null) ? mPageRtx
      .getName(pNameKey, pNodeKind) : currentNamePage.getName(pNameKey, pNodeKind);
  }

  @Override
  public int createNameKey(@Nullable final String pName, @Nonnull final ENode pNodeKind) throws TTIOException {
    checkNotNull(pNodeKind);
    final String string = (pName == null ? "" : pName);
    final int nameKey = NamePageHash.generateHashForString(string);
    final NamePage namePage = (NamePage)mNewRoot.getNamePageReference().getPage();
    namePage.setName(nameKey, string, pNodeKind);
    return nameKey;
  }

  @Override
  public void commit(final PageReference pReference) throws AbsTTException {
    IPage page = null;

    // if reference is not null, get one from the persistent storage.
    if (pReference != null) {
      // first, try to get one from the log
      final long nodePageKey = pReference.getNodePageKey();
      final NodePageContainer cont = mLog.get(nodePageKey);
      if (cont != null) {
        page = cont.getModified();
      }

      // if none is in the log, test if one is instantiated, if so, get
      // the one flexible from the reference
      if (page == null) {
        page = pReference.getPage();
        if (page == null) {
          return;
        }
      }

      pReference.setPage(page);
      // Recursively commit indirectely referenced pages and then
      // write self.
      page.commit(this);
      mPageWriter.write(pReference);
      pReference.setPage(null);
      // afterwards synchronize all logs since the changes must to be
      // written to the transaction log as
      // well
      if (cont != null) {
        mPageRtx.mSession.syncLogs(cont, mTransactionID);
      }
    }
  }

  @Override
  public UberPage commit() throws AbsTTException {
    mPageRtx.mSession.mCommitLock.lock();

    final PageReference uberPageReference = new PageReference();
    final UberPage uberPage = getUberPage();
    uberPageReference.setPage(uberPage);

    // // // /////////////
    // // // New code starts here
    // // // /////////////
    // final Stack<PageReference> refs = new Stack<PageReference>();
    // refs.push(uberPageReference);
    //
    // final Stack<Integer> refIndex = new Stack<Integer>();
    // refIndex.push(0);
    // refIndex.push(0);
    //
    // do {
    //
    // assert refs.size() + 1 == refIndex.size();
    //
    // // Getting the next ref
    // final PageReference currentRef = refs.peek();
    // final int currentIndex = refIndex.pop();
    //
    // // Check if referenced page is valid, if not, continue
    // AbsPage page = mLog.get(currentRef.getNodePageKey()).getModified();
    // boolean continueFlag = true;
    // if (page == null) {
    // if (currentRef.isInstantiated()) {
    // page = currentRef.getPage();
    // } else {
    // continueFlag = false;
    // }
    // } else {
    // currentRef.setPage(page);
    // }
    //
    // if (continueFlag) {
    //
    // if (currentIndex + 1 <= page.getReferences().length) {
    // // go down
    //
    // refIndex.push(currentIndex + 1);
    //
    // refs.push(page.getReference(currentIndex));
    // refIndex.push(0);
    //
    // } else {
    //
    // mPageWriter.write(currentRef);
    // refs.pop();
    // }
    //
    // } // ref is not designated to be serialized
    // else {
    // refs.pop();
    // }
    //
    // } while (!refs.empty());
    //
    // // // ///////////////
    // // // New code ends here
    // // // ///////////////

    // Recursively write indirectely referenced pages.
    uberPage.commit(this);

    uberPageReference.setPage(uberPage);
    mPageWriter.writeFirstReference(uberPageReference);
    uberPageReference.setPage(null);

    mPageRtx.mSession.waitForFinishedSync(mTransactionID);
    // mPageWriter.close();
    mPageRtx.mSession.mCommitLock.unlock();
    return uberPage;
  }

  @Override
  public void close() throws TTIOException {
    mPageRtx.clearCache();
    mLog.clear();
    mPageWriter.close();
  }

  /**
   * Prepare indirect page, that is getting the referenced indirect page or a new page.
   * 
   * @param pReference
   *          {@link PageReference} to get the indirect page from or to create a new one
   * @return {@link IndirectPage} reference
   * @throws TTIOException
   *           if an I/O error occurs
   */
  private IndirectPage prepareIndirectPage(@Nonnull final PageReference pReference) throws TTIOException {
    IndirectPage page = (IndirectPage)pReference.getPage();
    if (page == null) {
      if (pReference.getKey() == null) {
        page = new IndirectPage(getUberPage().getRevision());
      } else {
        page = new IndirectPage(mPageRtx.dereferenceIndirectPage(pReference), mNewRoot.getRevision() + 1);
      }
      pReference.setPage(page);
    }
    return page;
  }

  /**
   * Prepare node page.
   * 
   * @param pNodePageKey
   *          the key of the node page
   * @throws TTIOException
   *           if an I/O error occurs
   */
  private void prepareNodePage(final long pNodePageKey) throws TTIOException {
    // Last level points to node nodePageReference.
    NodePageContainer cont = mLog.get(pNodePageKey);
    if (cont == null) {
      // Indirect reference.
      final PageReference reference = prepareLeafOfTree(mNewRoot.getIndirectPageReference(), pNodePageKey);
      NodePage page = (NodePage)reference.getPage();

      if (page == null) {
        if (reference.getKey() == null) {
          cont = new NodePageContainer(new NodePage(pNodePageKey, IConstants.UBP_ROOT_REVISION_NUMBER));
        } else {
          cont = dereferenceNodePageForModification(pNodePageKey);
        }
      } else {
        cont = new NodePageContainer(page);
      }

      reference.setNodePageKey(pNodePageKey);
      mLog.put(pNodePageKey, cont);
    }
    assert cont != null;
    mNodePageCon = cont;
  }

  private RevisionRootPage preparePreviousRevisionRootPage(@Nonnegative final long pBaseRevision,
    @Nonnegative final long pRepresentRevision) throws TTIOException {
    if (getUberPage().isBootstrap()) {
      return mPageRtx.loadRevRoot(pBaseRevision);
    } else {
      // Prepare revision root nodePageReference.
      final RevisionRootPage revisionRootPage =
        new RevisionRootPage(mPageRtx.loadRevRoot(pBaseRevision), pRepresentRevision + 1);

      // Prepare indirect tree to hold reference to prepared revision root
      // nodePageReference.
      final PageReference revisionRootPageReference =
        prepareLeafOfTree(getUberPage().getIndirectPageReference(), getUberPage().getRevisionNumber());

      // Link the prepared revision root nodePageReference with the
      // prepared indirect tree.
      revisionRootPageReference.setPage(revisionRootPage);

      revisionRootPage.getNamePageReference().setPage(
        mPageRtx.getActualRevisionRootPage().getNamePageReference().getPage());

      // Return prepared revision root nodePageReference.
      return revisionRootPage;
    }
  }

  private PageReference prepareLeafOfTree(final PageReference pStartReference, final long pKey)
    throws TTIOException {

    // Initial state pointing to the indirect nodePageReference of level 0.
    PageReference reference = pStartReference;
    int offset = 0;
    long levelKey = pKey;

    // Iterate through all levels.
    for (int level = 0, height = IConstants.INP_LEVEL_PAGE_COUNT_EXPONENT.length; level < height; level++) {
      offset = (int)(levelKey >> IConstants.INP_LEVEL_PAGE_COUNT_EXPONENT[level]);
      levelKey -= offset << IConstants.INP_LEVEL_PAGE_COUNT_EXPONENT[level];
      final IndirectPage page = prepareIndirectPage(reference);
      reference = page.getReferences()[offset];
    }

    // Return reference to leaf of indirect tree.
    return reference;
  }

  /**
   * Dereference node page reference.
   * 
   * @param pNodePageKey
   *          key of node page
   * @return dereferenced page
   * @throws TTIOException
   *           if an I/O error occurs
   */
  private NodePageContainer dereferenceNodePageForModification(final long pNodePageKey) throws TTIOException {
    final NodePage[] revs = mPageRtx.getSnapshotPages(pNodePageKey);
    final ERevisioning revisioning = mPageRtx.mSession.mResourceConfig.mRevisionKind;
    final int mileStoneRevision = mPageRtx.mSession.mResourceConfig.mRevisionsToRestore;
    return revisioning.combinePagesForModification(revs, mileStoneRevision);
  }

  @Override
  public RevisionRootPage getActualRevisionRootPage() {
    return mNewRoot;
  }

  /**
   * Updating a container in this transaction state.
   * 
   * @param pCont
   *          {@link NodePageContainer} reference to be updated
   */
  public void updateDateContainer(final NodePageContainer pCont) {
    synchronized (mLog) {
      // TODO implement for MultiWriteTrans
      // Refer to issue #203
    }
  }

  /**
   * Building name consisting out of prefix and name. NamespaceUri is not used
   * over here.
   * 
   * @param pQName
   *          the {@link QName} of an element
   * @return a string with [prefix:]localname
   */
  public static String buildName(@Nonnull final QName pQName) {
    String name;
    if (pQName.getPrefix().isEmpty()) {
      name = pQName.getLocalPart();
    } else {
      name = new StringBuilder(pQName.getPrefix()).append(":").append(pQName.getLocalPart()).toString();
    }
    return name;
  }

  @Override
  public byte[] getRawName(final int pNameKey, @Nonnull final ENode pKind) {
    return mPageRtx.getRawName(pNameKey, pKind);
  }

  @Override
  public NodePageContainer getNodeFromPage(final long pKey) throws TTIOException {
    return mPageRtx.getNodeFromPage(pKey);
  }

  @Override
  public UberPage getUberPage() {
    return mPageRtx.getUberPage();
  }
}