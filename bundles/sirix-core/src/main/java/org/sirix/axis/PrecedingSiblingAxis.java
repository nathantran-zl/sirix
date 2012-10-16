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

package org.sirix.axis;

import org.sirix.api.NodeCursor;
import org.sirix.api.NodeReadTrx;
import org.sirix.node.Kind;
import org.sirix.settings.EFixed;

/**
 * <h1>PrecedingSiblingAxis</h1>
 * 
 * <p>
 * Iterate over all preceding siblings of kind ELEMENT or TEXT starting at a given node. Self is not included.
 * Note that the axis conforms to the XPath specification and returns nodes in document order.
 * </p>
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 */
public final class PrecedingSiblingAxis extends AbsAxis {

  /** Determines if it's the first call. */
  private boolean mIsFirst;

  /**
   * Constructor initializing internal state.
   * 
   * @param pRtx
   *          exclusive (immutable) trx to iterate with
   */
  public PrecedingSiblingAxis(final NodeCursor pRtx) {
    super(pRtx);
  }

  @Override
  public void reset(final long pNodeKey) {
    super.reset(pNodeKey);
    mIsFirst = true;
  }
  
  @Override
  protected long nextKey() {
    final NodeReadTrx rtx = getTrx();
    if (mIsFirst) {
      mIsFirst = false;
      /*
       * If the context node is an attribute or namespace node,
       * the following-sibling axis is empty.
       */
      final Kind kind = rtx.getKind();
      if (kind == Kind.ATTRIBUTE || kind == Kind.NAMESPACE) {
        return done();
      } else {
        if (rtx.hasParent()) {
          final long startNodeKey = rtx.getNodeKey();
          rtx.moveToParent();
          rtx.moveToFirstChild();

          if (rtx.getNodeKey() == startNodeKey) {
            return EFixed.NULL_NODE_KEY.getStandardProperty();
          } else {
            final long key = rtx.getNodeKey();
            rtx.moveTo(startNodeKey);
            return key;
          }
        }
      }
    }

    if (rtx.hasRightSibling() && rtx.getRightSiblingKey() != getStartKey()) {
      return rtx.getRightSiblingKey();
    }
    
    return done();
  }
}
