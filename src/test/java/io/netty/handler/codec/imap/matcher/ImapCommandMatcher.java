/*
 * Copyright 2016 Laurent Belmonte <laurent.belmonte@gmail.com>
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.imap.matcher;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import io.netty.handler.codec.imap.CommandParameter;
import io.netty.handler.codec.imap.ImapCommand;

public class ImapCommandMatcher extends TypeSafeMatcher<ImapCommand> {

	private Matcher<String> tagMatcher;
	private Matcher<String> commandMatcher;
	private List<Matcher<? extends CommandParameter>> parametersMatchers;
	private Matcher<Integer> parametersLengthMatcher;

	public ImapCommandMatcher(String tag, String command, CommandParameter[] parameters) {
		this.tagMatcher = equalTo(tag);
		this.commandMatcher = equalTo(command);

		this.parametersMatchers = new ArrayList<Matcher<? extends CommandParameter>>();
		if (parameters == null) {
			this.parametersLengthMatcher = equalTo(0);
		} else {
			this.parametersLengthMatcher = equalTo(parameters.length);
			this.parametersMatchers = new ArrayList<Matcher<? extends CommandParameter>>();
			for (CommandParameter param : parameters) {
				parametersMatchers.add(matchParam(param));
			}
		}
	}

	public static Matcher<? extends CommandParameter> matchParam(CommandParameter param) {
		return equalTo(param);
	}

	public void describeTo(Description description) {
		description.appendDescriptionOf(tagMatcher);
		description.appendDescriptionOf(commandMatcher);
		for (Matcher<?> pMatcher : parametersMatchers) {
			description.appendDescriptionOf(pMatcher);
		}
	}

	@Override
	public boolean matchesSafely(ImapCommand item) {
		boolean match = true;
		match &= tagMatcher.matches(item.getTag());

		match &= commandMatcher.matches(item.getCommand());
		match &= parametersLengthMatcher.matches(item.getParameters().size());

		if (match) {
			int i = 0;
			for (Matcher<?> pMatcher : parametersMatchers) {
				CommandParameter param = item.getParameters().get(i);
				match &= pMatcher.matches(param);
				i++;
			}
		}
		return match;
	}

}
