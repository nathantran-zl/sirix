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

package org.sirix.gui;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.sirix.access.DatabaseImpl;
import org.sirix.access.conf.SessionConfiguration;
import org.sirix.api.Database;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.Session;
import org.sirix.exception.SirixException;
import org.sirix.gui.view.model.TraverseCompareTree;
import org.sirix.utils.LogWrapper;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * <h1>ReadDB</h1>
 * 
 * <p>
 * Provides access to a sirix storage.
 * </p>
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public final class ReadDB implements AutoCloseable {

	/** {@link LogWrapper}. */
	private static final LogWrapper LOGWRAPPER = new LogWrapper(
			LoggerFactory.getLogger(TraverseCompareTree.class));

	/** Sirix {@link Database}. */
	private final Database mDatabase;

	/** Sirix {@link Session}. */
	private final Session mSession;

	/** Sirix {@link NodeReadTrx}. */
	private final NodeReadTrx mRtx;

	/** Revision number. */
	private final int mRevision;

	/** Compare revision. */
	private int mCompareRevision;

	/**
	 * Constructor.
	 * 
	 * @param pFile
	 *          The {@link File} to open.
	 * @throws SirixException
	 *           if anything went wrong while opening a file
	 */
	public ReadDB(final File pFile) throws SirixException {
		this(pFile, -1, 0);
	}

	/**
	 * Constructor.
	 * 
	 * @param pFile
	 *          The {@link File} to open.
	 * @param pRevision
	 *          The revision to open.
	 * @throws SirixException
	 *           if anything went wrong while opening a file
	 */
	public ReadDB(final File pFile, final int pRevision) throws SirixException {
		this(pFile, pRevision, 0);
	}

	/**
	 * Constructor.
	 * 
	 * @param pFile
	 *          The {@link File} to open.
	 * @param pRevision
	 *          The revision to open.
	 * @param pNodekeyToStart
	 *          The key of the node where the transaction initially has to move
	 *          to.
	 * @throws SirixException
	 *           if anything went wrong while opening a file
	 */
	public ReadDB(final @Nonnull File pFile, final @Nonnegative int pRevision,
			final long pNodekeyToStart) throws SirixException {
		checkNotNull(pFile);
		checkArgument(pRevision >= -1, "pRevision must be >= -1!");
		checkArgument(pNodekeyToStart >= 0, "pNodekeyToStart must be >= 0!");

		// Initialize database.
		mDatabase = DatabaseImpl.openDatabase(pFile);
		mSession = mDatabase
				.getSession(new SessionConfiguration.Builder("shredded").build());

		if (pRevision == -1) {
			// Open newest revision.
			mRtx = mSession.beginNodeReadTrx();
		} else {
			mRtx = mSession.beginNodeReadTrx(pRevision);
		}
		mRtx.moveTo(pNodekeyToStart);
		mRevision = mRtx.getRevisionNumber();
	}

	/**
	 * Get the {@link Database} instance.
	 * 
	 * @return the Database.
	 */
	public Database getDatabase() {
		return mDatabase;
	}

	/**
	 * Get the {@link Session} instance.
	 * 
	 * @return the Session.
	 */
	public Session getSession() {
		return mSession;
	}

	/**
	 * Get revision number.
	 * 
	 * @return current revision number or 0 if a SirixIOException occured
	 */
	public int getRevisionNumber() {
		return mRevision;
	}

	/**
	 * Set compare number.
	 * 
	 * @param pRevision
	 *          revision number to set
	 */
	public void setCompareRevisionNumber(final int pRevision) {
		checkArgument(pRevision > 0, "paramRevision must be > 0!");
		mCompareRevision = pRevision;
	}

	/**
	 * Get compare number.
	 */
	public int getCompareRevisionNumber() {
		return mCompareRevision;
	}

	/**
	 * Get current node key.
	 * 
	 * @return node key
	 */
	public long getNodeKey() {
		return mRtx.getNodeKey();
	}

	/**
	 * Set node key.
	 * 
	 * @param pNodeKey
	 *          node key
	 */
	public void setKey(final long pNodeKey) {
		mRtx.moveTo(pNodeKey);
	}

	/**
	 * Close all database related instances.
	 */
	@Override
	public void close() {
		try {
			mRtx.close();
			mSession.close();
			mDatabase.close();
		} catch (final SirixException e) {
			LOGWRAPPER.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean equals(final Object pObj) {
		if (this == pObj) {
			return true;
		}

		if (pObj instanceof ReadDB) {
			final ReadDB other = (ReadDB) pObj;
			return Objects.equal(mDatabase, other.mDatabase)
					&& Objects.equal(mSession, other.mSession)
					&& Objects.equal(mRtx, other.mRtx)
					&& Objects.equal(mRevision, other.mRevision)
					&& Objects.equal(mCompareRevision, other.mCompareRevision);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(mDatabase, mSession, mRtx, mRevision,
				mCompareRevision);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("database", mDatabase)
				.add("session", mSession).add("rtx", mRtx).add("revision", mRevision)
				.add("comp Revision", mCompareRevision).toString();
	}
}
