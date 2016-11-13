package io.netty.handler.codec.imap;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

public class ImapResponseDecoder extends ByteToMessageDecoder {

	private enum State {
		READ_TAG, READ_MAYBE_STATUS_CODE, READ_STATUS_CODE, READ_COMMAND, READ_PARAMTERS, READ_WOOT, READ_STATUS_CODE_PARAMETER, READ_MESSAGE_STATUS, READ_STATUS_REPONSE, READ_STATUS_PARAMETERS, READ_SERVER_RESPONSE, READ_END, READ_MESSAGE_STATUS_PARAMETERS
	}

	private State currentState;
	private AtomDecoder atomDecoder = new AtomDecoder();
	private ImapResponseBuilder builder;
	private ParameterDecoder paramDecoder = new ParameterDecoder(false);
	private ParameterDecoder paramStatusCodeDecoder = new ParameterDecoder(true);
	private LineDecoder lineDecoder = new LineDecoder();

	public ImapResponseDecoder() {
		resetNow();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		switch (currentState) {
		case READ_TAG: {
			String atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}

			atomDecoder.reset();
			if (atom.equals("*")) {
				builder.untagged();
			} else {
				builder.tag(atom);
			}

			checkAndSkipSpace(in);
			currentState = State.READ_WOOT;

		}
		default:
		}

		switch (currentState) {
		case READ_WOOT: {
			String atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}
			atomDecoder.reset();
			if (isNumber(atom)) {
				checkAndSkipSpace(in);
				currentState = State.READ_MESSAGE_STATUS;
				builder.messageStatusNumber(Integer.parseInt(atom));
				break;
			} else if (isStatusResponse(atom)) {
				checkAndSkipSpace(in);
				currentState = State.READ_MAYBE_STATUS_CODE;
				builder.statusResponse(atom);
			} else {
				currentState = State.READ_SERVER_RESPONSE;
				builder.serverResponseCommand(atom);
			}
		}
		default:
		}

		switch (currentState) {
		case READ_MAYBE_STATUS_CODE: {
			byte firstChar = in.getByte(in.readerIndex());
			if (firstChar != '[') {
				currentState = State.READ_STATUS_REPONSE;

			} else {
				in.skipBytes(1);
				currentState = State.READ_STATUS_CODE;
			}
		}
		default:
		}
		switch (currentState) {
		case READ_STATUS_CODE: {
			String atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}
			atomDecoder.reset();
			builder.statusCode(atom);
			currentState = State.READ_STATUS_CODE_PARAMETER;

		}
		case READ_STATUS_CODE_PARAMETER: {
			CommandParameter param = null;
			while ((param = paramStatusCodeDecoder.next(ctx, in)) != null) {
				builder.addStatusCodeParam(param);
			}

			if (paramStatusCodeDecoder.getState() == ParameterDecoder.State.Ended) {
				in.skipBytes(1);
				currentState = State.READ_STATUS_REPONSE;
			} else {
				return;
			}
		}
		default:
		}
		switch (currentState) {
		case READ_STATUS_REPONSE: {
			String message = readMessage(in);
			if (message == null) {
				return;
			}
			builder.statusReponseMessage(message);
			out.add(builder.build());
			resetNow();

			break;
		}
		case READ_SERVER_RESPONSE: {
			CommandParameter param = null;
			while ((param = paramDecoder.next(ctx, in)) != null) {
				builder.addReponseCommandParam(param);
			}

			if (paramDecoder.getState() == ParameterDecoder.State.Ended) {
				out.add(builder.build());
				resetNow();
				break;
			}
		}

		case READ_MESSAGE_STATUS: {
			String atom = atomDecoder.parse(in);
			if (atom == null) {
				return;
			}
			atomDecoder.reset();
			currentState = State.READ_MESSAGE_STATUS_PARAMETERS;
			builder.messageStatusCommand(atom);
		}
		case READ_MESSAGE_STATUS_PARAMETERS: {
			CommandParameter param = null;
			while ((param = paramDecoder.next(ctx, in)) != null) {
				builder.addReponseCommandParam(param);
			}

			if (paramDecoder.getState() == ParameterDecoder.State.Ended) {
				out.add(builder.build());
				resetNow();
				break;
			}
		}

		default:
		}

	}

	private void checkAndSkipSpace(ByteBuf in) {
		if (in.getByte(in.readerIndex()) == ' ') {
			in.skipBytes(1);
		} else {
			throw new CorruptedFrameException();
		}

	}

	private String readMessage(ByteBuf in) {
		String msg = lineDecoder.parse(in);
		if (msg != null) {
			lineDecoder.reset();
		}

		return msg;
	}

	private boolean isStatusResponse(String value) {
		return (value.equals("OK") || value.equals("NO") || value.equals("BAD") || value.equals("PREAUTH")
				|| value.equals("BYE"));
	}

	private boolean isNumber(String atom) {
		try {
			Integer.parseInt(atom);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void resetNow() {
		builder = new ImapResponseBuilder();
		atomDecoder.reset();
		currentState = State.READ_TAG;
		paramDecoder.reset();
		paramStatusCodeDecoder.reset();
		lineDecoder.reset();
	}

}
