package io.netty.handler.codec.imap;

import java.util.ArrayList;
import java.util.List;

public class ImapResponseBuilder {

	private String tag;
	private Integer messageStatusNumer;
	private String messageStatusCommand;
	private String serverResponseCommand;
	private List<CommandParameter> parameters = new ArrayList<>(10);
	private List<CommandParameter> statusCodeParams;
	private String statusCode;
	private String statusResponse;
	private String statusReponseMessage;
	private String statusResponseCommand;

	public void untagged() {
		this.tag = null;
	}

	public void tag(String value) {
		this.tag = value;
	}

	public void statusCode(String value) {
		this.statusCode = value;
	}

	public void addStatusCodeParam(CommandParameter param) {
		if (statusCodeParams == null) {
			statusCodeParams = new ArrayList<>();
		}

		statusCodeParams.add(param);
	}

	public void statusResponse(String value) {
		this.statusResponse = value;
	}

	public void statusReponseMessage(String message) {
		this.statusReponseMessage = message;
	}

	public void messageStatusNumber(int parseInt) {
		messageStatusNumer = parseInt;
	}

	public void messageStatusCommand(String value) {
		this.messageStatusCommand = value;
	}

	public void serverResponseCommand(String value) {
		this.serverResponseCommand = value;
	}

	public void addReponseCommandParam(CommandParameter param) {
		parameters.add(param);
	}

	public ImapResponse build() {
		if (messageStatusNumer != null) {
			return new ImapResponse.MessageStatusResponse(messageStatusNumer, messageStatusCommand, parameters);
		} else if (serverResponseCommand != null) {
			return new ImapResponse.ServerResponse(serverResponseCommand, parameters);
		} else {
			return ImapResponse.StatusResponse.create(tag, statusCode, statusCodeParams, statusResponse,
					statusReponseMessage);
		}
	}

	public boolean tagged() {
		return tag != null;
	}

}
