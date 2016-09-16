package lk.everbloom.api.handler;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.http.HttpStatus;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.templates.uri.URITemplateException;
import org.apache.synapse.commons.templates.uri.parser.Node;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.commons.templates.uri.parser.URITemplateParser;
import org.wso2.carbon.apimgt.gateway.handlers.Utils;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.TreeMap;

public class OptionalQueryParamWhiteListHandler extends AbstractHandler {
    private static final Log log = LogFactory.getLog(OptionalQueryParamWhiteListHandler.class);
    private String protectedResources = null;

    public boolean handleRequest(MessageContext messageContext) {
        log.info("===================handleRequest============================================================");
        URITemplateParser parser = new URITemplateParser();
        Node syntaxTree = null;
        ArrayList<String> whiteListedOptionalQueryParams = new ArrayList<String>();
        whiteListedOptionalQueryParams.add("shStore");
        whiteListedOptionalQueryParams.add("currency");
        try {
            String pathString = (String) ((Axis2MessageContext) messageContext).getProperty("REST_FULL_REQUEST_PATH");
            if (pathString != null) {
                syntaxTree = parser.parse(pathString);
                if (syntaxTree != null && syntaxTree.hasQueryTemplate()) {
                    for (String pair : (pathString.substring(pathString.indexOf("?") + 1)).split("&")) {
                        int eq = pair.indexOf("=");
                        if (!whiteListedOptionalQueryParams.contains(URLDecoder.decode(pair.substring(0, eq), "UTF-8"))) {
                            sendBadRequestTrace(messageContext, "Unwanted Optional Query Parameter in the request");
                            return false;
                        }
                    }
                }
            }
            // If no query parameter in the request just pass-through
            return true;
        } catch (URITemplateException ex) {
            sendBadRequestTrace(messageContext, "Unwanted Optional Query Parameter in the request");
            return false;
        } catch (Exception ex) {
            sendBadRequestTrace(messageContext, "Unwanted Optional Query Parameter in the request");
            return false;
        }
    }

    public boolean handleResponse(MessageContext messageContext) {
        return true;
    }

    private void sendBadRequestTrace(MessageContext messageContext,
                                     String errorMessage) {
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        try {
            RelayUtils.buildMessage(axis2MC);
        } catch (Exception ex) {
            log.error("Error occurred while building the message", ex);
        }
        TreeMap<String, String> headers = (TreeMap<String, String>) ((Axis2MessageContext) messageContext).
                getAxis2MessageContext().getProperty("TRANSPORT_HEADERS");

        if (headers != null && headers.get(HTTPConstants.HEADER_ACCEPT) != null && !headers.get(HTTPConstants.HEADER_ACCEPT).equals("*/*")) {
            axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE, headers.get(HTTPConstants.HEADER_ACCEPT));
        } else {
            axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_APPLICATION_JSON);
        }
        Utils.setSOAPFault(messageContext, "Client", "Unwanted Optional Query Parameter", "Unwanted Optional Query Parameter in the request");
        Utils.sendFault(messageContext, HttpStatus.SC_BAD_REQUEST);
    }
}
