package org.sirix.access;

import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.sirix.api.ItemList;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.Session;
import org.sirix.api.visitor.IVisitResult;
import org.sirix.api.visitor.IVisitor;
import org.sirix.exception.SirixException;
import org.sirix.exception.SirixIOException;
import org.sirix.node.Kind;
import org.sirix.node.interfaces.Node;
import org.sirix.service.xml.xpath.AtomicValue;

import com.google.common.collect.ForwardingObject;

/**
 * Forwards all methods to the delegate.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public abstract class AbsForwardingNodeReadTrx extends ForwardingObject
		implements NodeReadTrx {

	/** Constructor for use by subclasses. */
	protected AbsForwardingNodeReadTrx() {
	}

	@Override
	protected abstract NodeReadTrx delegate();

	@Override
	public ItemList<AtomicValue> getItemList() {
		return delegate().getItemList();
	}

	@Override
	public long getMaxNodeKey() throws SirixIOException {
		return delegate().getMaxNodeKey();
	}

	@Override
	public void close() throws SirixException {
		delegate().close();
	}
	
	@Override
	public String getNamespaceURI() {
		return delegate().getNamespaceURI();
	}

	@Override
	public QName getName() {
		return delegate().getName();
	}

	@Override
	public int getRevisionNumber() throws SirixIOException {
		return delegate().getRevisionNumber();
	}

	@Override
	public long getRevisionTimestamp() throws SirixIOException {
		return delegate().getRevisionTimestamp();
	}

	@Override
	public Session getSession() {
		return delegate().getSession();
	}

	@Override
	public long getTransactionID() {
		return delegate().getTransactionID();
	}

	@Override
	public String getType() {
		return delegate().getType();
	}

	@Override
	public String getValue() {
		return delegate().getValue();
	}

	@Override
	public boolean isClosed() {
		return delegate().isClosed();
	}

	@Override
	public int keyForName(final @Nonnull String pName) {
		return delegate().keyForName(pName);
	}

	@Override
	public Move<? extends NodeReadTrx> moveTo(long pKey) {
		return delegate().moveTo(pKey);
	}

	@Override
	public Move<? extends NodeReadTrx> moveToAttribute(
			final @Nonnegative int pIndex) {
		return delegate().moveToAttribute(pIndex);
	}

	@Override
	public Move<? extends NodeReadTrx> moveToAttributeByName(
			final @Nonnull QName pQName) {
		return delegate().moveToAttributeByName(pQName);
	}

	@Override
	public Move<? extends NodeReadTrx> moveToDocumentRoot() {
		return delegate().moveToDocumentRoot();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToFirstChild() {
		return delegate().moveToFirstChild();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToLeftSibling() {
		return delegate().moveToLeftSibling();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToNamespace(
			final @Nonnegative int pIndex) {
		return delegate().moveToNamespace(pIndex);
	}

	@Override
	public Move<? extends NodeReadTrx> moveToNextFollowing() {
		return delegate().moveToNextFollowing();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToParent() {
		return delegate().moveToParent();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToRightSibling() {
		return delegate().moveToRightSibling();
	}

	@Override
	public Move<? extends NodeReadTrx> moveToLastChild() {
		return delegate().moveToLastChild();
	}

	@Override
	public String nameForKey(final int pKey) {
		return delegate().nameForKey(pKey);
	}

	@Override
	public byte[] rawNameForKey(final int pKey) {
		return delegate().rawNameForKey(pKey);
	}

	@Override
	public NodeReadTrx cloneInstance() throws SirixException {
		return delegate().cloneInstance();
	}

	@Override
	public int getNameCount(final @Nonnull String pName,
			final @Nonnull Kind pKind) {
		return delegate().getNameCount(pName, pKind);
	}

	@Override
	public int getAttributeCount() {
		return delegate().getAttributeCount();
	}

	@Override
	public long getNodeKey() {
		return delegate().getNodeKey();
	}

	@Override
	public int getNamespaceCount() {
		return delegate().getNamespaceCount();
	}

	@Override
	public Kind getKind() {
		return delegate().getKind();
	}

	@Override
	public boolean hasFirstChild() {
		return delegate().hasFirstChild();
	}

	@Override
	public boolean hasLastChild() {
		return delegate().hasLastChild();
	}

	@Override
	public boolean hasLeftSibling() {
		return delegate().hasLeftSibling();
	}

	@Override
	public boolean hasRightSibling() {
		return delegate().hasRightSibling();
	}

	@Override
	public boolean hasParent() {
		return delegate().hasParent();
	}

	@Override
	public boolean hasNode(long pKey) {
		return delegate().hasNode(pKey);
	}

	@Override
	public long getAttributeKey(@Nonnegative int pIndex) {
		return delegate().getAttributeKey(pIndex);
	}

	@Override
	public long getFirstChildKey() {
		return delegate().getFirstChildKey();
	}

	@Override
	public long getLastChildKey() {
		return delegate().getLastChildKey();
	}

	@Override
	public long getLeftSiblingKey() {
		return delegate().getLeftSiblingKey();
	}

	@Override
	public int getNameKey() {
		return delegate().getNameKey();
	}

	@Override
	public long getParentKey() {
		return delegate().getParentKey();
	}

	@Override
	public boolean isNameNode() {
		return delegate().isNameNode();
	}

	@Override
	public Kind getPathKind() {
		return delegate().getPathKind();
	}

	@Override
	public long getPathNodeKey() {
		return delegate().getPathNodeKey();
	}

	@Override
	public long getRightSiblingKey() {
		return delegate().getRightSiblingKey();
	}

	@Override
	public int getTypeKey() {
		return delegate().getTypeKey();
	}

	@Override
	public IVisitResult acceptVisitor(@Nonnull IVisitor pVisitor) {
		return delegate().acceptVisitor(pVisitor);
	}
	
	@Override
	public boolean isValueNode() {
		return delegate().isValueNode();
	}
	
	@Override
	public boolean hasAttributes() {
		return delegate().hasAttributes();
	}
	
	@Override
	public boolean hasChildren() {
		return delegate().hasChildren();
	}
	
	@Override
	public Node getNode() {
		return delegate().getNode();
	}

	@Override
	public int getURIKey() {
		return delegate().getURIKey();
	}

	@Override
	public boolean isStructuralNode() {
		return delegate().isStructuralNode();
	}

	@Override
	public List<Long> getAttributeKeys() {
		return delegate().getAttributeKeys();
	}

	@Override
	public long getHash() {
		return delegate().getHash();
	}

	@Override
	public List<Long> getNamespaceKeys() {
		return delegate().getNamespaceKeys();
	}

	@Override
	public byte[] getRawValue() {
		return delegate().getRawValue();
	}

	@Override
	public long getChildCount() {
		return delegate().getChildCount();
	}

	@Override
	public long getDescendantCount() {
		return delegate().getDescendantCount();
	}
	
	@Override
	public Kind getFirstChildKind() {
		return delegate().getFirstChildKind();
	}
	
	@Override
	public Kind getLastChildKind() {
		return delegate().getLastChildKind();
	}
	
	@Override
	public Kind getLeftSiblingKind() {
		return delegate().getLeftSiblingKind();
	}
	
	@Override
	public Kind getRightSiblingKind() {
		return delegate().getRightSiblingKind();
	}
	
	@Override
	public Kind getParentKind() {
		return delegate().getParentKind();
	}
	
	@Override
	public boolean isAttribute() {
		return delegate().isAttribute();
	}
	
	@Override
	public boolean isComment() {
		return delegate().isComment();
	}
	
	@Override
	public boolean isDocumentRoot() {
		return delegate().isDocumentRoot();
	}
	
	@Override
	public boolean isElement() {
		return delegate().isElement();
	}
	
	@Override
	public boolean isNamespace() {
		return delegate().isNamespace();
	}
	
	@Override
	public boolean isPI() {
		return delegate().isPI();
	}
	
	@Override
	public boolean isText() {
		return delegate().isText();
	}
}
