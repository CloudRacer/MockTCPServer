package io.cloudracer;

public class HostMessageTypeConstants {

    public final static String VALID_TERMINATOR = "\r\n\n";

    // Test messages.
    public final static String MESSAGE_16 = ""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<QAStatusChange>"
            + "  <MessageHeader>"
            + "     <SESSION_KEY>RTCIS</SESSION_KEY>"
            + "     <MESSAGE_ID>00100370001071991191</MESSAGE_ID>"
            + "     <TIMESTAMP>20140713162212</TIMESTAMP>"
            + "   </MessageHeader>"
            + "  <ChangeULQA>"
            + "     <MESSAGE_TYPE>A16</MESSAGE_TYPE>"
            + "     <UNIT_LOAD_ID>00100370001071991207</UNIT_LOAD_ID>"
            + "     <BRAND_CODE>80228847</BRAND_CODE>"
            + "     <CODE_DATE>4209172765</CODE_DATE>"
            + "     <UL_HOLD_STATUS_CODE>RL</UL_HOLD_STATUS_CODE>"
            + "  </ChangeULQA>"
            + "</QAStatusChange>"
            + HostMessageTypeConstants.VALID_TERMINATOR;

    public final static String MESSAGE_8 = ""
            // + "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<RequestInduction>"
            + "  <MessageHeader>"
            + "     <SESSION_KEY>RTCIS</SESSION_KEY>"
            + "     <MESSAGE_ID>00000000000000000001</MESSAGE_ID>"
            + "     <TIMESTAMP>20140626150000</TIMESTAMP>"
            + "   </MessageHeader>"
            + "  <RequestLocForPallet>"
            + "     <MESSAGE_TYPE>A8</MESSAGE_TYPE>"
            + "     <UNIT_LOAD_ID>700370001646151000</UNIT_LOAD_ID>"
            + "     <BRAND_CODE>11111111</BRAND_CODE>"
            + "     <BRAND_DESCRIPTION>DOWNY LQSCP AF 4/103Z 120 LOADS</BRAND_DESCRIPTION>"
            + "     <CODE_DATE>415817020I</CODE_DATE>"
            + "     <PALLET_TYPE>U</PALLET_TYPE>"
            + "     <UL_HOLD_STATUS_CODE>RL</UL_HOLD_STATUS_CODE>"
            // + " <ACTIV_INPUT_LOCATION></ACTIV_INPUT_LOCATION>"
            + "     <ITEM_GROUP>ENHANC</ITEM_GROUP>"
            + "     <BASE_ULID>700370001646151000</BASE_ULID>"
            + "     <CASE_QUANTITY>50</CASE_QUANTITY>"
            + "     <PARTIAL_FLAG>N</PARTIAL_FLAG>"
            + "     <PLC_USERID>3100</PLC_USERID>"
            + "  </RequestLocForPallet>"
            + "</RequestInduction>"
            + HostMessageTypeConstants.VALID_TERMINATOR;

    public final static String MALFORMED_XML = ""
            // + "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<test><unterminated_element></INVALID_TERMINATOR></test>"
            + HostMessageTypeConstants.VALID_TERMINATOR;

    private final static String ASSIGN_INDUCTION_LOC_MESSAGE_NAME = "AssignInductionLoc";
    public final static String ASSIGN_INDUCTION_LOC_MESSAGE_PATTERN = String.format("<\\?xml version=\"1.0\" encoding=\"utf-8\"\\?>\\n<%s>(.*?(\\n))+.*?<\\/%s>\\n%s", ASSIGN_INDUCTION_LOC_MESSAGE_NAME, ASSIGN_INDUCTION_LOC_MESSAGE_NAME, HostMessageTypeConstants.VALID_TERMINATOR);

}