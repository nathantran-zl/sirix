package org.sirix.xquery.node;

import javax.annotation.Nullable;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Scope;
import org.brackit.xquery.xdm.Stream;
import org.sirix.api.XdmNodeReadTrx;
import org.sirix.api.XdmNodeWriteTrx;
import org.sirix.exception.SirixException;
import org.sirix.settings.Fixed;

/**
 * Sirix scope.
 * 
 * @author Johannes Lichtenberger
 *
 */
public final class SirixScope implements Scope {

	/** Sirix {@link XdmNodeReadTrx}. */
	private final XdmNodeReadTrx mRtx;

	/**
	 * Constructor.
	 * 
	 * @param node
	 *          database node
	 */
	public SirixScope(final DBNode node) {
		// Assertion instead of checkNotNull(...) (part of internal API).
		assert node != null;
		mRtx = node.getTrx();
	}

	@Override
	public Stream<String> localPrefixes() throws DocumentException {
		return new Stream<String>() {
			private int mIndex;

			private final int mNamespaces = mRtx.getNamespaceCount();

			@Override
			public String next() throws DocumentException {
				if (mIndex < mNamespaces) {
					mRtx.moveToNamespace(mIndex++);
					return mRtx.nameForKey(mRtx.getPrefixKey());
				}
				return null;
			}

			@Override
			public void close() {
			}
		};
	}

	@Override
	public String defaultNS() throws DocumentException {
		return resolvePrefix("");
	}

	@Override
	public void addPrefix(final String prefix, final String uri)
			throws DocumentException {
		if (mRtx instanceof XdmNodeWriteTrx) {
			final XdmNodeWriteTrx wtx = (XdmNodeWriteTrx) mRtx;
			try {
				wtx.insertNamespace(new QNm(uri, prefix, ""));
			} catch (final SirixException e) {
				throw new DocumentException(e);
			}
		}
	}

	@Override
	public String resolvePrefix(final @Nullable String prefix)
			throws DocumentException {
		final int prefixVocID = (prefix == null || prefix.isEmpty()) ? -1 : mRtx
				.keyForName(prefix);
		while (true) {
			// First iterate over all namespaces.
			for (int i = 0, namespaces = mRtx.getNamespaceCount(); i < namespaces; i++) {
				mRtx.moveToNamespace(i);
				final String name = mRtx.nameForKey(prefixVocID);
				if (name != null) {
					return name;
				}
				mRtx.moveToParent();
			}
			// Then move to parent.
			if (mRtx.hasParent()
					&& mRtx.getParentKey() != Fixed.NULL_NODE_KEY.getStandardProperty()) {
				mRtx.moveToParent();
			} else {
				break;
			}
		}
		if (prefix.equals("xml")) {
			return "http://www.w3.org/XML/1998/namespace";
		}
		return prefixVocID == -1 ? "" : null;
	}

	@Override
	public void setDefaultNS(final String uri) throws DocumentException {
		addPrefix("", uri);
	}
}
