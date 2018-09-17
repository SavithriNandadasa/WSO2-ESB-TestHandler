package com.se.ifw.esb.handler;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.AbstractSynapseHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.rest.RESTConstants;

public class SESynapseLogHandler extends AbstractSynapseHandler {


    /*
     * Logger instance.
     */
    private static final Log LOGGER =
            LogFactory.getLog(SESynapseLogHandler.class);

    private static final String SELOG_INFLOW_REQUEST_START_TIME = "SELOG_INFLOW_REQUEST_START_TIME";
    private static final String SELOG_OUTFLOW_REQUEST_START_TIME = "SELOG_OUTFLOW_REQUEST_START_TIME";
    private static final String SELOG_INFLOW_RESPONSE_END_TIME = "SELOG_INFLOW_RESPONSE_END_TIME";

    /**
     * Incoming request to the service or API. This is the first entry point,
     * in fact it is after Axis2 layer. This is where we will determine the
     * tracking id and log HTTP method and headers similar to wire log.
     *
     * @see <a href=
     * "https://docs.wso2.com/display/ESB490/Writing+a+Synapse+Handler">
     * WSO2 Docs</a>
     */
    public boolean handleRequestInFlow(MessageContext synCtx) {

        SynapseLog log = SELogTrackUtil.getLog(synCtx, LOGGER);
        synCtx.setProperty(SELOG_INFLOW_REQUEST_START_TIME, System.currentTimeMillis());

        log.auditDebug("Entering SESynapseLogHandler.handleRequestInFlow");

        try {
            //Set the logging context
            SELogTrackUtil.setLogContext(synCtx, log);

            //Log the request as per wire log
            StringBuilder msg = new StringBuilder();
            msg.append(">> ");
            msg.append(SELogTrackUtil.getHTTPMethod(synCtx));
            msg.append(" ");
            msg.append(SELogTrackUtil.getToHTTPAddress(synCtx));

            log.auditLog(msg);
            log.auditLog(">> HTTP Headers" + SELogTrackUtil.getHTTPHeaders(synCtx));
            log.auditDebug("Returning SESynapseLogHandler.handleRequestInFlow");
        } catch (Exception e) {

            log.auditWarn("Unable to set log context due to : " + e.getMessage());
        }
        //Always true
        return true;
    }

    /*
     * Outgoing request from the service to the backend. This is where we will
     * log the outgoing HTTP address and headers.
     *
     * @see <a href=
     *    "https://docs.wso2.com/display/ESB490/Writing+a+Synapse+Handler">
     *    WSO2 Docs</a>
     */
    public boolean handleRequestOutFlow(MessageContext synCtx) {

        SynapseLog log = SELogTrackUtil.getLog(synCtx, LOGGER);
        log.auditDebug("Entering SESynapseLogHandler.handleRequestOutFlow");
        synCtx.setProperty(SELOG_OUTFLOW_REQUEST_START_TIME, System.currentTimeMillis());

        try {
            //Set the logging context
            SELogTrackUtil.setLogContext(synCtx, log);

            //TODO figure out how to print back-end URL
            //Log the request as per wire log
            //StringBuilder msg = new StringBuilder();
            //msg.append("<<<< ");
            //msg.append(SELogTrackUtil.getHTTPMethod(synCtx));
            //msg.append(" ");
            //msg.append(SELogTrackUtil.getReplyToHTTPAddress(synCtx));
            //log.auditLog(msg);

            log.auditLog(">>>> HTTP Headers " + SELogTrackUtil.getHTTPHeaders(synCtx));
            log.auditDebug("Returning SESynapseLogHandler.handleRequestOutFlow");
        } catch (Exception e) {
            log.auditWarn("Unable to set log context due to : " + e.getMessage());
        }
        //Always true
        return true;
    }

    /**
     * Incoming response from backend to service. This is where we will
     * log the backend response headers and status.
     *
     * @see <a href=
     * "https://docs.wso2.com/display/ESB490/Writing+a+Synapse+Handler">
     * WSO2 Docs</a>
     */
    public boolean handleResponseInFlow(MessageContext synCtx) {

        SynapseLog log = SELogTrackUtil.getLog(synCtx, LOGGER);
        synCtx.setProperty(SELOG_INFLOW_RESPONSE_END_TIME, System.currentTimeMillis());
        log.auditDebug("Entering SESynapseLogHandler.handleResponseInFlow");
        try {
            //Set the logging context
            SELogTrackUtil.setLogContext(synCtx, log);
            log.auditLog("<<<< " + SELogTrackUtil.getHTTPStatusMessage(synCtx));
            log.auditLog("<<<< HTTP Headers " + SELogTrackUtil.getHTTPHeaders(synCtx));
            log.auditDebug("Returning SESynapseLogHandler.handleResponseInFlow");
        } catch (Exception e) {

            log.auditWarn(
                    "Unable to set log context due to : " + e.getMessage());
        }

        //Always true
        return true;
    }

    /**
     * Outgoing response from the service to caller. This is where we will log
     * the service response header and status.
     *
     * @see <a href=
     * "https://docs.wso2.com/display/ESB490/Writing+a+Synapse+Handler">
     * WSO2 Docs</a>
     */
    public boolean handleResponseOutFlow(MessageContext synCtx) {


        long responseTime = 0;
        long serviceTime = 0;
        long backendTime = 0;
        long backendEndTime = 0;
        long endTime = System.currentTimeMillis();
        SynapseLog log = SELogTrackUtil.getLog(synCtx, LOGGER);
        log.auditDebug("Entering SESynapseLogHandler.handleResponseOutFlow");

        try {
            //Set the logging context
            SELogTrackUtil.setLogContext(synCtx, log);
            //Log the request as per wire log

            log.auditLog("<< " + SELogTrackUtil.getHTTPStatusMessage(synCtx));
            log.auditLog("<< HTTP Headers " + SELogTrackUtil.getHTTPHeaders(synCtx));
            log.auditDebug("Returning SESynapseLogHandler.handleResponseOutFlow");

            long startTime =  Long.parseLong((String) (synCtx.getProperty(SELOG_INFLOW_REQUEST_START_TIME)));
            long backendStartTime =  Long.parseLong((String) (synCtx.getProperty(SELOG_OUTFLOW_REQUEST_START_TIME)));

            if (synCtx.getProperty(SELOG_INFLOW_RESPONSE_END_TIME) != null) {
                backendEndTime = Long.valueOf((String) (synCtx.getProperty(SELOG_INFLOW_RESPONSE_END_TIME)));
            }

            //When start time not properly set
            if (startTime == 0) {
                responseTime = 0;
                backendTime = 0;
                serviceTime = 0;
            } else if (endTime != 0 && backendStartTime != 0 && backendEndTime != 0) { //When
                // response caching is disabled
                responseTime = endTime - startTime;
                backendTime = backendEndTime - backendStartTime;
                serviceTime = responseTime - backendTime;

            } else if (endTime != 0 && backendStartTime == 0) {//When response caching enabled
                responseTime = endTime - startTime;
                serviceTime = responseTime;
                backendTime = 0;
                //cacheHit = true;
            }

            responseTime = endTime - startTime;


            String API_Name = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION);
            String HTTP_METHOD = (String) synCtx.getProperty(Constants.Configuration.HTTP_METHOD);

            String CONTEXT = (String) synCtx.getProperty(RESTConstants.REST_API_CONTEXT);

            String HTTP_RESPONSE_STATUS_CODE = SELogTrackUtil.getHTTPStatusMessage(synCtx);


            //	String Application_Name=(String) synCtx.getProperty(APIMgtGatewayConstants.APPLICATION_NAME);


            String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);


            String ERROR_CODE = String.valueOf(synCtx.getProperty(SynapseConstants.ERROR_CODE));
            String ERROR_MESSAGE = (String) synCtx.getProperty(SynapseConstants.ERROR_MESSAGE);


            //newly Added
            String FULL_REQUEST_PATH = (String) synCtx.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);
            String SUB_PATH = (String) synCtx.getProperty(RESTConstants.REST_SUB_REQUEST_PATH);


            log.auditLog("API Transaction Details:"
                    + "API_Name: " + API_Name + ",HTTP_METHOD: " + HTTP_METHOD + ", CONTEXT: " + CONTEXT + ", resourceName: " + resourceName +
                    ", HTTP_RESPONSE_STATUS_CODE: " + HTTP_RESPONSE_STATUS_CODE + ", ResponseTime: " + responseTime + ", BackendTime: " + backendTime + ", ServiceTime: " + serviceTime +
                    ", ERROR_CODE: " + ERROR_CODE + ", ERROR_MESSAGE: " + ERROR_MESSAGE + ",FULL_REQUEST_PATH" + FULL_REQUEST_PATH + ",SUB_PATH: " + SUB_PATH);

        } catch (Exception e) {

            log.auditWarn(
                    "Unable to set log context due to : " + e.getMessage());


        }

        //Always true
        return true;
    }
}
