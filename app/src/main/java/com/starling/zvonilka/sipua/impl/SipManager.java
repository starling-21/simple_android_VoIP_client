package com.starling.zvonilka.sipua.impl;


import android.content.Context;
import android.gov.nist.javax.sip.SipStackExt;
import android.gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import android.gov.nist.javax.sip.header.CSeq;
import android.gov.nist.javax.sip.header.SIPHeader;
import android.gov.nist.javax.sip.header.SIPHeaderNames;
import android.gov.nist.javax.sip.header.ims.SIPHeaderNamesIms;
import android.gov.nist.javax.sip.message.SIPMessage;
import android.gov.nist.javax.sip.stack.SIPDialogEventListener;
import android.gov.nist.javax.sip.stack.SIPTransactionStack;
import android.javax.sip.ClientTransaction;
import android.javax.sip.Dialog;
import android.javax.sip.DialogState;
import android.javax.sip.DialogTerminatedEvent;
import android.javax.sip.IOExceptionEvent;
import android.javax.sip.InvalidArgumentException;
import android.javax.sip.ListeningPoint;
import android.javax.sip.PeerUnavailableException;
import android.javax.sip.RequestEvent;
import android.javax.sip.ResponseEvent;
import android.javax.sip.ServerTransaction;
import android.javax.sip.SipException;
import android.javax.sip.SipFactory;
import android.javax.sip.SipListener;
import android.javax.sip.SipProvider;
import android.javax.sip.SipStack;
import android.javax.sip.TimeoutEvent;
import android.javax.sip.Transaction;
import android.javax.sip.TransactionDoesNotExistException;
import android.javax.sip.TransactionTerminatedEvent;
import android.javax.sip.TransactionUnavailableException;
import android.javax.sip.address.Address;
import android.javax.sip.address.AddressFactory;
import android.javax.sip.address.SipURI;
import android.javax.sip.address.URI;
import android.javax.sip.header.CSeqHeader;
import android.javax.sip.header.CallIdHeader;
import android.javax.sip.header.ContactHeader;
import android.javax.sip.header.ContentTypeHeader;
import android.javax.sip.header.ExpiresHeader;
import android.javax.sip.header.FromHeader;
import android.javax.sip.header.Header;
import android.javax.sip.header.HeaderFactory;
import android.javax.sip.header.MaxForwardsHeader;
import android.javax.sip.header.RouteHeader;
import android.javax.sip.header.ToHeader;
import android.javax.sip.header.ViaHeader;
import android.javax.sip.message.MessageFactory;
import android.javax.sip.message.Request;
import android.javax.sip.message.Response;

import com.starling.zvonilka.sipua.ISipEventListener;
import com.starling.zvonilka.sipua.ISipManager;
import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.NetworkUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;


/**
 * Created by starling on 1/3/2018.
 * this is SIP CORE of this app
 */

public class SipManager implements SipListener, ISipManager {

    private static SipManager instance = null;
    private boolean initialized;

    public SipStack sipStack;
    public SipProvider sipProvider;
    public SipFactory sipFactory;
    public HeaderFactory headerFactory;
    public AddressFactory addressFactory;
    public MessageFactory messageFactory;


    public ListeningPoint udpListeningPoint;
    private SipProfile sipProfile;
    private Context context;

    //---------------my variables for SipManager
    private static ServerTransaction currentServerTransaction;
    private static ClientTransaction currentClientTransaction;

    private CallDirection callDirection = CallDirection.NONE;
    private SipManagerState sipManagerState = SipManagerState.OFFLINE;

    private ArrayList<ISipEventListener> customSipEventListeners = new ArrayList<>();
    private ISipEventListener customSipEventListener;

    //-------------------------------------------------------------------------

    //--------------- temp variables -------------------

    //------------------------------------------------


    enum CallDirection {
        NONE,
        INCOMING,
        OUTGOING,
    }


    public static SipManager getInstance() {
        if (instance == null) {
            instance = new SipManager();
        }
        return instance;
    }


    protected SipManager() {
    }

    @Override
    public void register() {
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.REGISTERING);
//        sendRegister(1200);//<- 20 min
//        sendRegister(600);// <- 10 min
//        sendRegister(300);// <- 5 min
        sendRegister(120);// <- 2 min
//        sendRegister(60);//  <- 1 min
    }

    @Override
    public void unregister() {
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.UNREGISTERING);
        sendUnregister();
    }

    @Override
    public void call(String toNumber) {
        setCallDirection(CallDirection.OUTGOING);
        setSipManagerState(SipManagerState.OUTCOING_CALLING);
        sendInvite(toNumber);
    }

    @Override
    public void cancelCalling() {
        dispatchCustomSipEvent(CustomSipEvent.CANCELL_CALLING, null);
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
        sendCancel();
    }

    @Override
    public void hangUp() {
        dispatchCustomSipEvent(CustomSipEvent.HANG_UP, null);

        if (callDirection == CallDirection.OUTGOING) {
            sendBuy(currentClientTransaction);
        } else if (callDirection == CallDirection.INCOMING) {
            sendBuy(currentServerTransaction);
        }
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
    }

    @Override
    public void acceptCall() {
        setCallDirection(CallDirection.INCOMING);
        setSipManagerState(SipManagerState.ESTABLISHING);
        sendOkForInvite();
    }

    @Override
    public void rejectCall() {
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_CALL_REJECTED, null);
        sendDecline();
    }

    @Override
    public void sendMessage() {
        //TODO implement message sending
    }

    /**
     * first starting point
     * into the SipManager
     *
     * @param context
     */
    public void init(Context context) {
        this.context = context;
        sipProfile = new SipProfile(context);
        if (!initialized) {
            initialized = initializeSipStack();
        } else {
            reinitializeSipStack();
        }
    }

    /**
     * reinitialize sip stack affter Internet onnection has been lost
     */
    public void reinitializeSipStack() {
        Logg.ing("RE_INITIALIZING SIP STACK:");
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.OFFLINE);

        initialized = initializeSipStack();
        register();
    }

    /**
     * initializeSipStack sipStack, create sipProvider,
     * udpListeneing point and adding this class as SIP event listener
     *
     * @return
     */
    private boolean initializeSipStack() {
        Logg.ing("INITIALIZING SIP STACK:");

        try {
            String ipAddress = NetworkUtil.getLocalIpAddress(context.getApplicationContext());
            sipProfile.setLocalIp(ipAddress);

            sipFactory = SipFactory.getInstance();
            sipFactory.resetFactory();
            sipFactory.setPathName("android.gov.nist");
            Properties properties = new Properties();
            properties.setProperty("android.javax.sip.OUTBOUND_PROXY", sipProfile.getRemoteEndpoint() + "/" + sipProfile.getTransport());
            properties.setProperty("android.javax.sip.STACK_NAME", "androidSip");

            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

            sipStack = sipFactory.createSipStack(properties);
            Logg.ing("SipStack created");

            //adding sipListener and checking condition for avoding duplicate listeners createion (just 1 account at 1 time?
            if (sipStack.getSipProviders() != null) {
                while (sipStack.getSipProviders().hasNext()) {
                    SipProvider sipProvider = (SipProvider) sipStack.getSipProviders().next();
                    ListeningPoint[] listeningPoints = sipProvider.getListeningPoints();
                    for (ListeningPoint listeningPoint : listeningPoints) {
                        sipProvider.removeListeningPoint(listeningPoint);
                        sipStack.deleteListeningPoint(listeningPoint);
                    }
                    sipProvider.removeSipListener(this);
                    sipStack.deleteSipProvider(sipProvider);
                }
            }

            //creating new udpListening point
//            if (udpListeningPoint != null) {
//                Logg.ing("SipStack, removing udpListeningPoint " + udpListeningPoint.getIPAddress() + ":" + udpListeningPoint.getPort());
//                sipStack.deleteListeningPoint(udpListeningPoint);
//                sipProvider.removeSipListener(this);
//            }

            sipProfile.generateNewLocalSipPort();
            udpListeningPoint = sipStack.createListeningPoint(sipProfile.getLocalIp(), sipProfile.getLocalSipPort(), sipProfile.getTransport());
            Logg.ing("udpListeningPoint created: " + udpListeningPoint.getIPAddress() + ":" + udpListeningPoint.getPort());


            sipProvider = sipStack.createSipProvider(udpListeningPoint);

            sipProvider.addSipListener(this);
            Logg.ing("added sip listener to sipStack");
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return false;
        } catch (Exception e) {
            setInitialized(false);
            System.out.println("initializeSipStack sipManager exception: ");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * stop sip stack and
     * release all resources related with it
     */
    public void stopSipStack() {
        try {
            ((SIPTransactionStack) sipStack).closeAllSockets(); //test line

            Iterator<SipProvider> sipProviderIterator = sipStack.getSipProviders();
            while (sipProviderIterator.hasNext()) {
                SipProvider sipProvider = sipProviderIterator.next();
                ListeningPoint[] listeningPoints = sipProvider.getListeningPoints();
                for (ListeningPoint listeningPoint : listeningPoints) {
                    sipProvider.removeListeningPoint(listeningPoint);
                    sipStack.deleteListeningPoint(listeningPoint);
                }
                sipProvider.removeSipListener(this);
                sipStack.deleteSipProvider(sipProvider);
                sipProviderIterator = sipStack.getSipProviders();
            }
            sipStack.stop();
            setInitialized(false);

            setCallDirection(CallDirection.NONE);
            setSipManagerState(SipManagerState.OFFLINE);
            dispatchCustomSipEvent(CustomSipEvent.UNREGISTERED, null);
        } catch (Exception e) {
            System.out.println("Cant remove the listening points or sip providers" + e);
            e.printStackTrace();
        }
    }

    //TODO refactor the method
    public void setInitialized(boolean state) {
        initialized = state;
        setSipManagerState(SipManagerState.OFFLINE);
        dispatchCustomSipEvent(CustomSipEvent.UNREGISTERED, null);
    }


    public boolean isInitialized() {
        return initialized;
    }

    public SipManagerState getSipManagerState() {
        return sipManagerState;
    }


    /**
     * process incoming Sip request
     *
     * @param requestEvent
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();

        CSeq cSeqHeader = (CSeq) request.getHeader(CSeq.NAME);
        String cSeqHeaderMethod = cSeqHeader.getMethod();


        Logg.ing("INCOMMING____REQUEST: " + cSeqHeaderMethod + "" + "\n\n" + requestEvent.getRequest().toString());
        //notifyGUI("REQUEST:\n" + requestEvent.getRequest().toString());
        //==========================================================================================
        //==========================================================================================


        //process incoming INVITE request
        if (request.getMethod().equals(Request.INVITE)) {
            currentServerTransaction = serverTransaction;
            incomingInvite(requestEvent, serverTransaction);
        }
        //process incoming OPTIONS request
        else if (request.getMethod().equals(Request.OPTIONS)) {
            incomingOptions(requestEvent);
        }
        //process incoming CANCELL
        else if (request.getMethod().equals(Request.CANCEL)) {
            currentServerTransaction = serverTransaction;
            incomingCancel(requestEvent, requestEvent.getDialog());
        }
        //process incoming BYE
        else if (request.getMethod().equals(Request.BYE)) {
            currentServerTransaction = serverTransaction;
            incomingBye(request, serverTransaction);
        }
        //process incoming ACK for accepting a call
        else if (request.getMethod().equals(Request.ACK)) {
            incomingAck(requestEvent);
        }
    }

    /**
     * process incoming Sip responese
     *
     * @param responseEvent
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        CSeq cSeqHeader = (CSeq) response.getHeader(CSeq.NAME);
        String cSeqHeaderMethod = cSeqHeader.getMethod();

        Logg.ing("INCOMING RESPONCE for: " + cSeqHeader.getMethod() + "      status_code=" +
                response.getStatusCode() + "\n\n" + response.toString());
        //notifyGUI("RESPONSE:\n" + response.toString());
        //******************************************************************************************
        //******************************************************************************************

        Dialog responseDialog = null;
        ClientTransaction tid = responseEvent.getClientTransaction();
        if (tid != null) {
            responseDialog = tid.getDialog();
        } else {
            responseDialog = responseEvent.getDialog();
        }

        //------------------------------------------------------------------------------------------
        //process all incoming registration  response events
        if (cSeqHeaderMethod.equals(Request.REGISTER)) {
            incomingRegister(responseEvent);
        }
        //------------------------------------------------------------------------------------------

        //process all incoming INVITE responce events
        else if (cSeqHeaderMethod.equals(Request.INVITE)) {

            //process TRYING during calling
            if (response.getStatusCode() == Response.TRYING) {
                Logg.ing("incoming response for INVITE, PROXY TRYING");
            }

            //process RINGING during calling
            else if (response.getStatusCode() == Response.RINGING) {
                Logg.ing("incoming response for INVITE, REMOTE RINGING");
                dispatchCustomSipEvent(CustomSipEvent.OUTGOING_CALL_STARTED, null);
            }

            //process incoming OK during calling, remote user picked up the phone
            else if (response.getStatusCode() == Response.OK) {
                Logg.ing("incoming response for INVITE, OK <-- remote site ACCEPTED the CALL");
                sendAck(responseEvent, responseDialog);
            }

            //process DECLINE during calling //TODO REad about decline
            else if (response.getStatusCode() == Response.DECLINE) {
                Logg.ing("incoming response for INVITE, timeout");
                incomingDecline();
            }


            //process request terminated
            else if (response.getStatusCode() == Response.REQUEST_TERMINATED) {
                //TODO request terminated
            }

            //process incoming FORBIDDEN (our call rejected by remote site)
            else if (response.getStatusCode() == Response.FORBIDDEN) {
                incomingForbidden();
            }

            //process TEMPORARILY_UNAVAILABLE during calling
            else if (response.getStatusCode() == Response.TEMPORARILY_UNAVAILABLE) {
                Logg.ing("incoming response for INVITE, TEMPORARILY_UNAVAILABLE");
                dispatchCustomSipEvent(CustomSipEvent.INCOMING_TEMPORARY_UNAVALIABLE, null);
            }

            //process BUSY_HERE during calling
            else if (response.getStatusCode() == Response.BUSY_HERE) {
                Logg.ing("incoming response for INVITE, BUSY_HERE");
                dispatchCustomSipEvent(CustomSipEvent.INCOMING_BUSY, null);
            }

            //process TIMEOUT REQUEST  during calling //TODO implement
            else if (response.getStatusCode() == Response.REQUEST_TIMEOUT) {
                Logg.ing("incoming response for INVITE, REQUEST_TIMEOUT");
            }

            //process unauthorized during calling
            else if (response.getStatusCode() == Response.UNAUTHORIZED ||
                    response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
                Logg.ing("incoming response for INVITE, UNAUTHORIZED");
                sendUserCredential(responseEvent);
            }
        }
        //------------------------------------------------------------------------------------------


    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        Logg.ing("Transaction on_processTimeout");
        Request request;
        if (timeoutEvent.isServerTransaction()) {
            request = timeoutEvent.getServerTransaction().getRequest();
        } else {
            request = timeoutEvent.getClientTransaction().getRequest();
        }
        Logg.ing("processTimeout(): method: " + request.getMethod() + " URI: " + request.getRequestURI());


        if (request.getMethod() == Request.INVITE) {
//            dispatchSipError(ISipEventListener.ErrorContext.ERROR_CONTEXT_CALL, RCClient.ErrorCodes.SIGNALLING_TIMEOUT,
//                    "Timed out waiting on " + request.getMethod());
            Logg.ing("Timed out waiting on " + request.getMethod());
        } else {
//            dispatchSipError(ISipEventListener.ErrorContext.ERROR_CONTEXT_NON_CALL, RCClient.ErrorCodes.SIGNALLING_TIMEOUT,
//                    "Timed out waiting on " + request.getMethod());
            Logg.ing("Timed out waiting on " + request.getMethod());
        }
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        Logg.ing("SipManager.processIOException: " + exceptionEvent.toString() + "\n" +
                "\thost: " + exceptionEvent.getHost() + "\n" +
                "\tport: " + exceptionEvent.getPort());
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        Logg.ing("SipManager.processTransactionTerminated:");
        if (transactionTerminatedEvent.isServerTransaction()) {
            Logg.ing("server transaction, METHOD = " + transactionTerminatedEvent.getServerTransaction().getRequest().getMethod());
        } else if (transactionTerminatedEvent.getClientTransaction() != null) {
            Logg.ing("server transaction, METHOD =" + transactionTerminatedEvent.getClientTransaction().getRequest().getMethod());
        }
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        Logg.ing("SipManager.processDialogTerminated: state=" + dialogTerminatedEvent.getDialog().getState());
    }


    /**
     * sending registration request to UAS
     *
     * @param expireSeconds - intrval in seconds
     */
    //TODO change to private
    private void sendRegister(final int expireSeconds) {
        //TODO remove initializeSipStack, it shoud be automatically when networ connectivity changed
        if (!initialized) {
            Logg.ing("REGISTRATION FAILED, sip stack not initialized");
            //reinitializeSipStack();
            return;
        }

        Thread thread = new Thread() {
            public void run() {
                try {
                    SipManager sipManager = SipManager.getInstance();

                    SipProvider sipProvider = sipManager.sipProvider;
                    AddressFactory addressFactory = sipManager.addressFactory;
                    MessageFactory messageFactory = sipManager.messageFactory;
                    HeaderFactory headerFactory = sipManager.headerFactory;

                    // Create addresses and via header for the request
                    Address fromAddress = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getSipUserName() +
                            "@" + sipManager.getSipProfile().getRemoteIp());
                    fromAddress.setDisplayName(sipManager.getSipProfile().getSipUserName());

                    Address toAddress = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getSipUserName() +
                            "@" + sipManager.getSipProfile().getRemoteIp());
                    toAddress.setDisplayName(sipManager.getSipProfile().getSipUserName());

                    Address contactAddress = createContactAddress();
                    ArrayList<ViaHeader> viaHeaders = createViaHeader();
                    URI requestURI = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getRemoteEndpoint()).getURI();
                    // Build the request
                    final Request request = messageFactory.createRequest(
                            requestURI,
                            Request.REGISTER,
                            sipProvider.getNewCallId(),
                            headerFactory.createCSeqHeader(1l, Request.REGISTER),
                            headerFactory.createFromHeader(fromAddress, "c3ff411e"),
                            headerFactory.createToHeader(toAddress, null),
                            viaHeaders,
                            headerFactory.createMaxForwardsHeader(70));
                    // Add the contact header
                    request.addHeader(headerFactory.createContactHeader(contactAddress));
                    request.addHeader(headerFactory.createExpiresHeader(expireSeconds));

                    // Send the request statefully, through the client transaction.
                    ClientTransaction transaction = sipProvider.getNewClientTransaction(request);
                    transaction.sendRequest();

                    // Print the request
                    Logg.ing("sending REGISTER_request:\n\n" + request.toString());
                    //SipManager.notifyGUI("sending REGISTER_request:\n" + request.toString());
                } catch (Exception e) {
                    setInitialized(false);
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * send user credential when Unauthorized received during registration process
     *
     * @param responseEvent
     */
    //TODO unauthorized also comes for INVITE request
    private void sendUserCredential(final ResponseEvent responseEvent) {
        Logg.ing("on sendUserCredential");

        if (!isInitialized())
            return;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                Response response = responseEvent.getResponse();
                ClientTransaction clientTransaction = responseEvent.getClientTransaction();
                AuthenticationHelper authenticationHelper = ((SipStackExt) sipStack)
                        .getAuthenticationHelper(new JainSipAccountManagerImpl(getSipProfile().getSipUserName(),
                                getSipProfile().getRemoteIp(),
                                getSipProfile().getSipPassword()), headerFactory);
                try {
                    ClientTransaction registrationClientTransaction = authenticationHelper.handleChallenge(response, clientTransaction, sipProvider, 5);
                    Logg.ing("SENDING REGISTRATION CREDENTIAL: \n\n" + registrationClientTransaction.getRequest().toString());

                    registrationClientTransaction.sendRequest();

                    // IMPORTANT! as UA respond vith user credential for REGISTER(unauthorized) response and INVITE(unauthorized)
                    // this prevent from assigning currentClientTransaction vith options_response_client_transaction
                    // for cancelling outgoing call request
                    if (((CSeq) response.getHeader(CSeq.NAME)).getMethod().equalsIgnoreCase(Request.INVITE)) {
                        currentClientTransaction = registrationClientTransaction;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }


    /**
     * sending unregistration request to UAS
     */
    //TODO change to private
    private void sendUnregister() {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    SipManager sipManager = SipManager.getInstance();

                    SipProvider sipProvider = sipManager.sipProvider;
                    MessageFactory messageFactory = sipManager.messageFactory;
                    HeaderFactory headerFactory = sipManager.headerFactory;
                    AddressFactory addressFactory = sipManager.addressFactory;

                    // Create addresses and via header for the request
                    Address fromAddress = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getSipUserName() +
                            "@" + sipManager.getSipProfile().getRemoteIp());
                    fromAddress.setDisplayName(sipManager.getSipProfile().getSipUserName());

                    Address toAddress = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getSipUserName() +
                            "@" + sipManager.getSipProfile().getRemoteIp());
                    toAddress.setDisplayName(sipManager.getSipProfile().getSipUserName());

                    ArrayList<ViaHeader> viaHeaders = createViaHeader();
                    URI requestURI = addressFactory.createAddress("sip:" + sipManager.getSipProfile().getRemoteEndpoint()).getURI();

                    // Build the request
                    final Request request = messageFactory.createRequest(
                            requestURI,
                            Request.REGISTER,
                            sipProvider.getNewCallId(),
                            headerFactory.createCSeqHeader(1l, Request.REGISTER),
                            headerFactory.createFromHeader(fromAddress, "c3ff411e"),
                            headerFactory.createToHeader(toAddress, null),
                            viaHeaders,
                            headerFactory.createMaxForwardsHeader(70));
                    // Add the contact header for unregister request
                    request.addHeader(headerFactory.createContactHeader(addressFactory.createAddress("*")));
                    request.addHeader(headerFactory.createExpiresHeader(0));

                    // Print the request
                    Logg.ing("sending UNREGISTER request:" + request.toString());
                    //SipManager.notifyGUI("sending UNREGISTER_request:\n" + request.toString());

                    // Send the request statefully, through the client transaction.
                    ClientTransaction transaction = sipProvider.getNewClientTransaction(request);
                    transaction.sendRequest();
                    //TODO change sipManager state - like UNREGISTERING
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * sending invite to calee via UAS
     *
     * @param to - calee sip string number
     */
    private void sendInvite(final String to) {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    //TO header
                    URI toURI = addressFactory.createURI("sip:" + to + "@" + getSipProfile().getRemoteIp());
                    Address toAddress = addressFactory.createAddress(toURI);
                    toAddress.setDisplayName(to);
                    ToHeader toHeader = headerFactory.createToHeader(toAddress, null);//"TOt0ZEP92");

                    //FROM header
                    URI fromURI = addressFactory.createURI("sip:" + sipProfile.getSipUserName() + "@" + getSipProfile().getLocalIp());
                    Address fromAddress = addressFactory.createAddress(fromURI);
                    fromAddress.setDisplayName(getSipProfile().getSipUserName());
                    FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, "Tzt0ZEP92");

                    //new call id header
                    CallIdHeader callIdHeader = sipProvider.getNewCallId();

                    //CSec header
                    CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1l, Request.INVITE);

                    //via headers
                    ArrayList<ViaHeader> viaHeader = createViaHeader();

                    //maxForwardHeader
                    MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);


                    Request inviteRequest = messageFactory.createRequest(toURI,
                            Request.INVITE,
                            callIdHeader,
                            cSeqHeader,
                            fromHeader,
                            toHeader,
                            viaHeader,
                            maxForwardsHeader);

                    //Route header
                    SipURI routeUri = addressFactory.createSipURI(null, getSipProfile().getRemoteIp());
                    routeUri.setTransportParam(getSipProfile().getTransport());
                    routeUri.setLrParam();
                    routeUri.setPort(getSipProfile().getRemotePort());

                    Address routeAddress = addressFactory.createAddress(routeUri);
                    RouteHeader route = headerFactory.createRouteHeader(routeAddress);
                    inviteRequest.addHeader(route);


                    //ContentType header
                    ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
                    inviteRequest.addHeader(contentTypeHeader);

                    //Contact Header
                    ContactHeader contactHeader = headerFactory.createContactHeader(createContactAddress());
                    inviteRequest.addHeader(contactHeader);


                    //getting NAT binding
//                    MappedAddress mappedAddress = StunUtil.getMappedAddress();
//                    EventBus.getDefault().post(new MyTestSipEvent(mappedAddress.getAddress() + ":" + mappedAddress.getPort()));

                    //create SDP header
                    String sdpData = "v=0\r\n" +
                            "o=- 13760799956958020 13760799956958020" + " IN IP4 " + sipProfile.getLocalIp() + "\r\n" +
                            "s=mysession session\r\n" +
                            "c=IN IP4 " + sipProfile.getLocalIp() + "\r\n" +
                            "t=0 0\r\n" +
                            "m=audio " + "12000" + " RTP/AVP 96\r\n" +
                            "a=rtpmap:96 opus/48000/2\r\n" +
                            "a=ptime:20\r\n";

                    byte[] contents = sdpData.getBytes();

                    inviteRequest.setContent(contents, contentTypeHeader);

                    ClientTransaction inviteTransaction = sipProvider.getNewClientTransaction(inviteRequest);
                    currentClientTransaction = inviteTransaction;
                    //TODO change sipManager status - CALLING
                    Logg.ing("SENDING INVITE:\n\n" + inviteRequest.toString());
                    inviteTransaction.sendRequest();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * send CANCEL for previously sent INVITE
     */
    private void sendCancel() {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Request canselRequest = currentClientTransaction.createCancel();
                    ClientTransaction cancelClientTransaction = sipProvider.getNewClientTransaction(canselRequest);
                    Logg.ing("sending CANCEL for outgoing call");
                    Logg.ing(canselRequest.toString());
                    cancelClientTransaction.sendRequest();
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    /**
     * send ACK after called UA accepted the call
     * as new ClientTransaction
     */
    private void sendAck(final ResponseEvent responseEvent, final Dialog dialog) {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Response response = responseEvent.getResponse();
                    SIPMessage sipMessage = (SIPMessage) response;
                    CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

                    Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                    Logg.ing("SENDING ACK request\n\n" + ackRequest.toString());
                    dialog.sendAck(ackRequest);

                    dispatchCustomSipEvent(CustomSipEvent.OUTGOING_CALL_CONFIRMED, sipMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        setCallDirection(CallDirection.OUTGOING);
        setSipManagerState(SipManagerState.ESTABLISHED);
    }

    /**
     * UA sends 487 Request terminated for incoming cancel request
     * if he has not sent  OK (200) for incoming INVITE
     */
    private void sendRequestTerminated(final RequestEvent requestEvent) {
        if (!isInitialized())
            return;

        final Thread thread = new Thread() {
            public void run() {
                try {
                    Response response = messageFactory.createResponse(Response.REQUEST_TERMINATED, requestEvent.getRequest());
                    requestEvent.getServerTransaction().sendResponse(response);
                    Logg.ing("SENDING Request Terminated 487\n" + response.toString());
                    //TODO change status to READY (canceled incoming call)
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * Decline incoming call
     */
    private void sendDecline() {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Response declineResponse = messageFactory.createResponse(Response.DECLINE,
                            currentServerTransaction.getRequest());
                    currentServerTransaction.sendResponse(declineResponse);
                    Logg.ing("SENDING DECLINE:\n" + declineResponse);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    /**
     * Send BUSY for incoming call
     */
    private void sendBusyHere() {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Response responseBusyHere = messageFactory.createResponse(Response.BUSY_HERE,
                            currentServerTransaction.getRequest());
                    currentServerTransaction.sendResponse(responseBusyHere);
                    Logg.ing("SENDING BUSY_HERE:\n" + responseBusyHere);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * send OK (200) for incoming INVITE
     */
    private void sendOkForInvite() {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                dispatchCustomSipEvent(CustomSipEvent.CALL_ACCEPT_SENT, null);

                Response responseOk;
                try {
                    responseOk = messageFactory.createResponse(Response.OK,
                            currentServerTransaction.getRequest());
                    //------------------------------------------------
                    //adding contact header
                    Address address = createContactAddress();
                    ContactHeader contactHeader = headerFactory.createContactHeader(address);
                    responseOk.addHeader(contactHeader);

                    //marking to contact header as Application is supposed to do it
                    ToHeader toHeader = (ToHeader) responseOk.getHeader(ToHeader.NAME);
                    toHeader.setTag("ko4321"); // Application is supposed to set.
                    responseOk.addHeader(contactHeader);


                    String sdpData = "v=0\r\n" +
                            "o=4855 13760799956958020 13760799956958020" + " IN IP4 " + sipProfile.getLocalIp() + "\r\n" +
                            "s=mysession session\r\n" +
                            "p=+46 8 52018010\r\n" +
                            "c=IN IP4 " + sipProfile.getLocalIp() + "\r\n" +
                            "t=0 0\r\n" +
                            "m=audio " + "15000" + " RTP/AVP 96\r\n" + //TODO change rtp port  //String.valueOf(port)
                            "a=rtpmap:96 opus/48000/2\r\n" +
                            "a=ptime:20\r\n";

                    byte[] contents = sdpData.getBytes();

                    ContentTypeHeader contentTypeHeader = headerFactory
                            .createContentTypeHeader("application", "sdp");
                    responseOk.setContent(contents, contentTypeHeader);

                    currentServerTransaction.sendResponse(responseOk);
                    Logg.ing("SENDING OK(for INVITE)\n\n" + responseOk);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * sending OK for BYE
     *
     * @param request
     * @param serverTransaction
     */
    private void sendOkForBye(final Request request, final ServerTransaction serverTransaction) {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    Response responseOk = messageFactory.createResponse(Response.OK, request);
                    serverTransaction.sendResponse(responseOk);
                    Logg.ing("SENDING OK(BYE)\n\n" + responseOk);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * send BYE to finish call
     */
    private void sendBuy(final Transaction transaction) {
        if (!isInitialized())
            return;

        Thread thread = new Thread() {
            public void run() {
                try {
                    final Dialog dialog = transaction.getDialog();
                    if (dialog == null) {
                        Logg.ing("Hmm, weird: dialog is already terminated - avoiding sending BYE");
                    } else {
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction byeTransaction = sipProvider.getNewClientTransaction(byeRequest);
                        dialog.sendRequest(byeTransaction);
                    }
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    /**
     * process incoming UA registration response
     *
     * @param responseEvent
     */
    private void incomingRegister(ResponseEvent responseEvent) {
        Logg.ing("on incoming Register, status=" + responseEvent.getResponse().getStatusCode());
        Response response = responseEvent.getResponse();

        if (response.getStatusCode() == Response.UNAUTHORIZED || response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED) {
            setSipManagerState(SipManagerState.UNAUTHORIZED);
            sendUserCredential(responseEvent);
        } else if (response.getStatusCode() == Response.OK) {
            ExpiresHeader expiresHeader = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
            //TODO implement ISipEventListener.onSipMessage()

            //successful or registration or unregistration
            if (expiresHeader.getExpires() > 0) {
                setSipManagerState(SipManagerState.READY);
                dispatchCustomSipEvent(CustomSipEvent.REGISTERED, null);
            } else if (expiresHeader.getExpires() == 0) {
                stopSipStack();
            }
        }
    }

    /**
     * sending OK response for incoming OPTIONS (sip-ping)
     *
     * @param requestEvent
     */
    private void incomingOptions(final RequestEvent requestEvent) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Request request = requestEvent.getRequest();
                try {
                    SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                    Response response = messageFactory.createResponse(Response.OK, request);
                    sipProvider.sendResponse(response);
                    Logg.ing("sending OK for options");
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    /**
     * process incoming INVITE REQUEST
     *
     * @param requestEvent
     * @param serverTransaction
     */
    private void incomingInvite(RequestEvent requestEvent, ServerTransaction serverTransaction) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        try {

            if (serverTransaction == null) {
                serverTransaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
            }
            if (serverTransaction == null)
                return;

            currentServerTransaction = serverTransaction;


            Response response = messageFactory.createResponse(Response.RINGING, serverTransaction.getRequest());
            serverTransaction.sendResponse(response);
            //SipManager.notifyGUI("sending RINGING for INVITE____" + response.toString());
            Logg.ing("sending RINGING for INVITE\n    " + response.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCallDirection(CallDirection.INCOMING);
        setSipManagerState(SipManagerState.INCOMING_CALLING);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_CALL, (SIPMessage) requestEvent.getRequest());
    }


    /**
     * process incoming CANCELL request
     *
     * @param requestEvent
     */
    private void incomingCancel(RequestEvent requestEvent, final Dialog dialog) {
        Logg.ing("on incomingCancell");
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_CANCEL, null);
        try {
            if (dialog.getState() == DialogState.CONFIRMED) {
                Logg.ing("Sending BYE -- for incoming CANCEL went in too late (user picked up the call)!!");
                Request byeRequest = dialog.createRequest(Request.BYE);

                ClientTransaction cancelClientTransaction = sipProvider.getNewClientTransaction(byeRequest);
                dialog.sendRequest(cancelClientTransaction);
                Logg.ing("SENDING BYE for Cancell (call has been established)");
            } else {
                //TODO maybe this response sends server
                sendRequestTerminated(requestEvent);
            }
        } catch (TransactionDoesNotExistException e) {
            e.printStackTrace();
        } catch (TransactionUnavailableException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
    }


    /**
     * process incoming BYE to finish a call
     *
     * @param request
     * @param serverTransaction
     */
    private void incomingBye(Request request, ServerTransaction serverTransaction) {
        Logg.ing("on Incomiing BYE");
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_BYE, null);

        sendOkForBye(request, serverTransaction);
    }


    /**
     * process incoming ACK which means that our call is accepted by remote side
     *
     * @param requestEvent
     */
    private void incomingAck(RequestEvent requestEvent) {
        Logg.ing("on incoming ACK, call accepted");
        setSipManagerState(SipManagerState.ESTABLISHED);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_CALL_CONFIRMED, null);
    }

    /**
     * process incoming Decline, when remote dont pick up the phone
     */
    private void incomingDecline() {
        Logg.ing("on Incomiing DECLINE");
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_TIMEOUT, null);
    }


    /**
     * process incoming forbidden response, usualy happens when remote site REJECT out call
     */
    private void incomingForbidden() {
        Logg.ing("on Incomiing FORBIDDEN");
        setCallDirection(CallDirection.NONE);
        setSipManagerState(SipManagerState.READY);
        dispatchCustomSipEvent(CustomSipEvent.INCOMING_FORBIDDEN, null);
    }


    private SipProfile getSipProfile() {
        return sipProfile;
    }


    /**
     * create VIA header list for SIP requests
     *
     * @return
     */
    private ArrayList<ViaHeader> createViaHeader() {
        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
        ViaHeader myViaHeader;
        try {
            SipManager sipManager = SipManager.getInstance();
            myViaHeader = sipManager.headerFactory.createViaHeader(sipManager.getSipProfile().getLocalIp(),
                    sipManager.getSipProfile().getLocalSipPort(),
                    sipManager.getSipProfile().getTransport(), null);
            myViaHeader.setRPort();
            viaHeaders.add(myViaHeader);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
        return viaHeaders;
    }

    /**
     * create local contact address
     * local Address object
     *
     * @return
     */
    private Address createContactAddress() {
        try {
            return this.addressFactory.createAddress("sip:"
                    + getSipProfile().getSipUserName() + "@"
                    + getSipProfile().getLocalEndpoint() + ";transport=udp"
                    + ";registering_acc=hello_fucker :D");
        } catch (ParseException e) {
            return null;
        }
    }

    private void setCallDirection(CallDirection callDirection) {
        this.callDirection = callDirection;
    }

    private void setSipManagerState(SipManagerState sipManagerState) {
        this.sipManagerState = sipManagerState;
    }

    /**
     * trensfer sip message to higher leve sip event handler
     *
     * @param customSipEvent
     * @param sipMessage
     */
    public void dispatchCustomSipEvent(CustomSipEvent customSipEvent, SIPMessage sipMessage) {
        if (customSipEventListeners.size() == 0)
            return;

        for (ISipEventListener sipEventListener : customSipEventListeners) {
            sipEventListener.onCustomSipEvent(customSipEvent, sipMessage);
        }
    }

    /**
     * add ISipEventListener class as higher sip events handler
     *
     * @param sipEventListener
     */
    public synchronized void addCustomSipEventListener(ISipEventListener sipEventListener) {
        //TODO now it's just 1 listener (kostul)
        customSipEventListeners.clear();
        customSipEventListeners.add(sipEventListener);

        setCustomSipEventListeners(sipEventListener);//test SipEventListener
    }

    public void setCustomSipEventListeners(ISipEventListener sipEventListener) {
        this.customSipEventListener = sipEventListener;
    }

    public ISipEventListener getCustomSipEventListener() {
        return this.customSipEventListener;
    }


}
