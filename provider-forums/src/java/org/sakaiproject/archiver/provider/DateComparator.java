/*
 * Copyright (c) Orchestral Developments Ltd and the Orion Health group of companies (2001 - 2017).
 *
 * This document is copyright. Except for the purpose of fair reviewing, no part
 * of this publication may be reproduced or transmitted in any form or by any
 * means, electronic or mechanical, including photocopying, recording, or any
 * information storage and retrieval system, without permission in writing from
 * the publisher. Infringers of copyright render themselves liable for
 * prosecution.
 */
package org.sakaiproject.archiver.provider;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.sakaiproject.api.app.messageforums.Message;

public class DateComparator implements Comparator<Message> {

	@Override
	public int compare(final Message m1, final Message m2) {
		return new CompareToBuilder()
				.append(m1.getCreated(), m2.getCreated())
				.toComparison();
	}

}
