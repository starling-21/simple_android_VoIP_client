/**
 * Java RTP Library (jlibrtp)
 * Copyright (C) 2006 Arne Kepp
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.starling.zvonilka.net.jlibrtp;

import java.util.Comparator;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PktBuffer stores packets either for buffering purposes,
 * or because they need to be assimilated to create a complete frame.
 * 
 * This behavior can be controlled through rtpSession.pktBufBehavior()
 * 
 * It optionally drops duplicate packets.
 * 
 * Note that newest is the most recently received, i.e. highest timeStamp
 * Next means new to old (from recently received to previously received) 
 * 
 * @author Arne Kepp
 */
public class PktBuffer {
	private static Logger logger = LoggerFactory.getLogger(PktBuffer.class);
	private static Comparator<RtpPkt> rtpPktComparator = new Comparator<RtpPkt>() {
		
		@Override
		public int compare(RtpPkt pkt1, RtpPkt pkt2) {
			long a = pkt1.getSeqNumber();
			long b = pkt2.getSeqNumber();

			if (a == b)
				return 0;
			else if (a > b) {
				if (a - b < 32768)
					return 1;
				else
					return -1;
			} else // a < b
			{
				if (b - a < 32768)
					return -1;
				else
					return 1;
			}
		}
	};
	
	/** The RTPSession holds information common to all packetBuffers, such as max size */
	private RTPSession rtpSession;
	/** SSRC of the the participant that this buffer is for */
	private long SSRC;
	/** The parent participant */
	private Participant p;
	
	private boolean isStarted = false;
	private long exceptSeqNumber = -1;
	private TreeSet<RtpPkt> jitterBuffer = new TreeSet<RtpPkt>(rtpPktComparator);
	
	/** 
	 * Creates a new PktBuffer, a linked list of PktBufNode
	 * 
	 * @param rtpSession the parent RTPSession
	 * @param p the participant to which this packetbuffer belongs.
	 * @param aPkt The first RTP packet, to be added to the buffer 
	 */
	protected PktBuffer(RTPSession rtpSession, Participant p, RtpPkt aPkt) {
		this.rtpSession = rtpSession;
		this.p = p;
		SSRC = aPkt.getSsrc();
		
		addPkt(aPkt);
	}

	/**
	 * Adds a packet, this happens in constant time if they arrive in order.
	 * Optimized for the case where each pkt is a complete frame.
	 * 
	 * @param aPkt the packet to be added to the buffer.
	 * @return integer, negative if operation failed (see code)
	 */
	protected synchronized void addPkt(RtpPkt aPkt) {
		if(aPkt == null) {
			logger.warn("fail to add null to jitter buffer");
			return;
		}
		
		if (aPkt.getSsrc() != SSRC) {
			logger.warn("SSRCs don't match, except = {}, but real is {}!", SSRC, aPkt.getSsrc());
			return;
		}
		
		// aPkt < except
		if (isStarted && aPkt.getSeqNumber() < exceptSeqNumber
				&& exceptSeqNumber - getBufferSize() < aPkt.getSeqNumber()) {
			logger.debug("ignore {}", aPkt);
			return;
		}
		
		bufferedAddPkt(aPkt);
	}
	
	private void bufferedAddPkt(RtpPkt aPkt) {
		
		boolean success = jitterBuffer.add(aPkt);
		if (!success) {
			jitterBuffer.remove(aPkt);
			jitterBuffer.add(aPkt);
			logger.debug("Replaced  {} ", aPkt);
		}
	}
	


	/** 
	 * Checks the oldest frame, if there is one, sees whether it is complete.
	 * @return Returns null if there are no complete frames available.
	 */
	public synchronized RtpPkt popOldestFrame() {
		int maxBufferSize = 0;
		if (jitterBuffer.size() > 0) {
			maxBufferSize = getBufferSize();
		}
		
		if (jitterBuffer.isEmpty()) {
			return null;
		}
		
		if (!isStarted) {
			exceptSeqNumber = rtpSession.appIntf.getFirstSeqNumber();
			if (-1 != exceptSeqNumber) {
				while(!jitterBuffer.isEmpty() 
						&& jitterBuffer.first().getSeqNumber() < exceptSeqNumber) {
					RtpPkt pkt = jitterBuffer.pollFirst();
					logger.info("ignore {}", pkt);
				}

				if (!jitterBuffer.isEmpty()) {
					exceptSeqNumber = jitterBuffer.first().getSeqNumber();
				}
				isStarted = true;
				logger.info("seqNoStart = {}", exceptSeqNumber);
			}
		}
		
		RtpPkt pop = null;
		if (null == pop && exceptSeqNumber == jitterBuffer.first().getSeqNumber()){
			pop = jitterBuffer.pollFirst();
		}
		
		if(null == pop && jitterBuffer.size() >= maxBufferSize) {
			pop = jitterBuffer.pollFirst();
		} 
		
		if (null != pop) {
			if (exceptSeqNumber != pop.getSeqNumber()) {
				logger.debug("maybe rtp lost, wait  {}, size = {}", exceptSeqNumber, jitterBuffer.size());
			}
			exceptSeqNumber = (pop.getSeqNumber() + 1) & 0xFFFFFFFF;
		}

		return pop;
	}

	private int getBufferSize() {
		return rtpSession.appIntf.getBufferSize();
	}
 	/** 
	 * Returns the length of the packetbuffer.
	 * @return number of frames (complete or not) in packetbuffer.
	 */
	public synchronized int getLength() {
		return jitterBuffer.size();
	}
}
