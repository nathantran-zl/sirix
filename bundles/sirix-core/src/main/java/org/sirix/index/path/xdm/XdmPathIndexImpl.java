package org.sirix.index.path.xdm;

import org.sirix.api.PageWriteTrx;
import org.sirix.index.IndexDef;
import org.sirix.index.path.PathIndexBuilderFactory;
import org.sirix.index.path.PathIndexListenerFactory;
import org.sirix.index.path.summary.PathSummaryReader;
import org.sirix.node.interfaces.Record;
import org.sirix.page.UnorderedKeyValuePage;

public final class XdmPathIndexImpl implements XdmPathIndex {

  private final PathIndexBuilderFactory mPathIndexBuilderFactory;

  private final PathIndexListenerFactory mPathIndexListenerFactory;

  public XdmPathIndexImpl() {
    mPathIndexBuilderFactory = new PathIndexBuilderFactory();
    mPathIndexListenerFactory = new PathIndexListenerFactory();
  }

  @Override
  public XdmPathIndexBuilder createBuilder(final PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
      final PathSummaryReader pathSummaryReader, final IndexDef indexDef) {
    final var builderDelegate = mPathIndexBuilderFactory.create(pageWriteTrx, pathSummaryReader, indexDef);
    return new XdmPathIndexBuilder(builderDelegate);
  }

  @Override
  public XdmPathIndexListener createListener(final PageWriteTrx<Long, Record, UnorderedKeyValuePage> pageWriteTrx,
      final PathSummaryReader pathSummaryReader, final IndexDef indexDef) {
    final var listenerDelegate = mPathIndexListenerFactory.create(pageWriteTrx, pathSummaryReader, indexDef);
    return new XdmPathIndexListener(listenerDelegate);
  }

}
