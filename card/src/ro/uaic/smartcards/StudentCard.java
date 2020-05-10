package ro.uaic.smartcards;

/** 
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * 
 */

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;


public class StudentCard extends Applet
{
    /* constants declaration */

    // code of CLA byte in the command APDU header
    final static byte StudentCard_CLA = (byte) 0x80;

    // codes of INS byte in the command APDU header
    final static byte GET_GRADE = (byte) 0x0A;
    final static byte GET_GRADES = (byte) 0x0B;
    final static byte GET_STUDENT_ID = (byte) 0x0C;

    final static byte INSERT_GRADE = (byte) 0x1A;
    final static byte INSERT_GRADES = (byte) 0x1B;

    final static byte VERIFY_PIN = (byte) 0x20;
    final static byte UPDATE_PIN = (byte) 0x70;
    
    //for testing purposes
    final static byte VERIFY_STUDENT_ID = (byte) 0xC0;
    final static byte VERIFY_STUDENT_GRADE = (byte) 0xCF;

    // maximum number of incorrect tries before the
    // PIN is blocked
    final static byte PIN_TRY_LIMIT = (byte) 0x04;
    // maximum size PIN
    final static byte MAX_PIN_SIZE = (byte) 0x08;

    // signal that the PIN verification failed
    final static short SW_VERIFICATION_FAILED = 0x6300;

    // signal the the PIN validation is required
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;

    
    // signal for when the PIN is entered wrong more than 3 times
    final static short SW_SECURITY_STATUS_NOT_SATISFIED = 0x63FF;
    
    final static short WRONG_STUDENT_ID_LENGTH = 0x7000;
    final static short WRONG_STUDENT_ID= 0x700A;
    final static short WRONG_NUMBER_OF_GRADES = 0x700B;
    
    final static short MAX_NUMBER_OF_STUDENTS = 3000;
    
    final static short DATE_lENGTH = 10;

    /* instance variables declaration */
    OwnerPIN pin;
    byte[] studentId = new byte[2];

    private final byte firstStudyFieldCode = 0x65;
    private short firstStudyFieldGrade;
    private byte[] firstStudyFieldDate;

    private final byte secondStudyFieldCode = 0x66;
    private short secondStudyFieldGrade;
    private byte[] secondStudyFieldDate;

    private final byte thirdStudyFieldCode = 0x67;
    private short thirdStudyFieldGrade;
    private byte[] thirdStudyFieldDate;

    private final byte fourthStudyFieldCode = 0x68;
    private short fourthStudyFieldGrade;
    private byte[] fourthStudyFieldDate;

    private final byte fifthStudyFieldCode = 0x69;
    private short fifthStudyFieldGrade;
    private byte[] fifthStudyFieldDate;

    private StudentCard(byte[] bArray, short bOffset, byte bLength)
    {
        // It is good programming practice to allocate
        // all the memory that an applet needs during
        // its lifetime inside the constructor
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

        byte iLen = bArray[bOffset]; // aid length
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // info length
        bOffset = (short) (bOffset + cLen + 1);
        byte pinLength = bArray[bOffset]; // pin data length
        short pinOffsset = (short) (bOffset + 1);

        // The installation parameters contain the PIN
        // initialization value and the studentId
        pin.update(bArray, pinOffsset, pinLength);
        
        short studentIdLengthOffset = (short) (bOffset + 1 + pinLength);
        byte studentIdLength =   bArray[studentIdLengthOffset];
        
        if (studentIdLength > 2 || studentIdLength < 1)
        {
            ISOException.throwIt(WRONG_STUDENT_ID_LENGTH);
        }
        
        short studentIdOffset =  (short) (studentIdLengthOffset + 1);
        Util.arrayCopy(bArray, studentIdOffset, studentId, (short) 0, studentIdLength);    
        register();

    } // end of the constructor

    public static void install(byte[] bArray, short bOffset, byte bLength)
    {
        // create a Wallet applet instance
        new StudentCard(bArray, bOffset, bLength);
    } // end of install method

    @Override
    public boolean select()
    {
        // The applet declines to be selected
        // if the pin is blocked.
        if (pin.getTriesRemaining() == 0)
        {
            return false;
        }

        return true;

    }// end of select method

    @Override
    public void deselect()
    {
        // reset the pin value
        pin.reset();

    }

    @Override
    public void process(APDU apdu)
    {
        // APDU object carries a byte array (buffer) to
        // transfer incoming and outgoing APDU header
        // and data bytes between card and CAD

        // At this point, only the first header bytes
        // [CLA, INS, P1, P2, P3] are available in
        // the APDU buffer.
        // The interface javacard.framework.ISO7816
        // declares constants to denote the offset of
        // these bytes in the APDU buffer

        byte[] buffer = apdu.getBuffer();
        // check SELECT APDU command

        if (apdu.isISOInterindustryCLA())
        {
            if (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4))
            {
                return;
            }
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        // verify the reset of commands have the
        // correct CLA byte, which specifies the
        // command structure
        if (buffer[ISO7816.OFFSET_CLA] != StudentCard_CLA)
        {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buffer[ISO7816.OFFSET_INS])
            {
            case GET_GRADES:
                getGrades(apdu);
                return;
            case INSERT_GRADES:
                insertGrades(apdu);
                return;
            case VERIFY_PIN:
                verifyPin(apdu);
                return;
            case UPDATE_PIN:
                updatePin(apdu);
                return;
            case VERIFY_STUDENT_ID:
                verifyStudentId(apdu);
                return;
            case VERIFY_STUDENT_GRADE:
                verifyStudentGrade(apdu);
            case GET_STUDENT_ID:
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }

    } // end of process method

    private void updatePin(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        // get the current pin length and offset
        byte currentPinLengthOffset = (byte) (ISO7816.OFFSET_LC + 1);
        byte currentlPinLength = buffer[currentPinLengthOffset];
        this.verify(buffer, (byte) (currentPinLengthOffset + 1), currentlPinLength);

        byte newPinLengthOffset = (byte) (currentPinLengthOffset + currentlPinLength + 1);
        byte newPinLength = buffer[newPinLengthOffset];
        byte newPinOffset = (byte) (newPinLengthOffset + 1);
        this.pin.update(buffer, (short) (newPinOffset), newPinLength);
    }
    
    private void getStudentId(APDU apdu)
    {
        byte[] buffer = apdu.getBuffer();
        
        // inform system that the applet has finished
        // processing the command and the system should
        // now prepare to construct a response APDU
        // which contains data field
        short le = apdu.setOutgoing();

        if (le < 2)
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 2);

        // move the balance data into the APDU buffer
        // starting at the offset 0
        buffer = studentId;

        // send the 2-byte balance at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 2);
    }

    private void verify(byte[] buffer, byte offset, byte pinLength)
    {
        if (this.pin.check(buffer, offset, pinLength) == false)
        {
            if (this.pin.getTriesRemaining() == 0)
            {
                // SW_SECURITY_STATUS_NOT_SATISFIED = 0x63FF;
                ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
            }
            ISOException.throwIt(SW_VERIFICATION_FAILED);
        }
    }
    
    private void verifyStudentId(APDU apdu)
    {
        if (!pin.isValidated())
        {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        
        byte[] buffer = apdu.getBuffer(); 
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // retrieve the STUDENT_ID data for validation.
        short studentIdLengthOffset = ISO7816.OFFSET_CDATA - 1;
        short studentIdLength = buffer[studentIdLengthOffset];
        if (studentIdLength > 2 || studentIdLength < 1)
        {
            ISOException.throwIt(WRONG_STUDENT_ID_LENGTH);
        }
        
        byte[] receivedStudentId = {0x00, 0x00};
        Util.arrayCopy(buffer, (short)(studentIdLengthOffset + 1), receivedStudentId, (short) 0, studentIdLength);
        if (receivedStudentId[0] != this.studentId[0] || receivedStudentId[1] != this.studentId[1])
        {
            ISOException.throwIt(WRONG_STUDENT_ID);
        }
    }
    
    private void verifyStudentGrade(APDU apdu)
    {
        if (!pin.isValidated())
        {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        
        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if (numBytes != byteRead)
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
    }

    private void verifyPin(APDU apdu)
    {

        byte[] buffer = apdu.getBuffer();
        // retrieve the PIN data for validation.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // check pin
        // the PIN data is read into the APDU buffer
        // at the offset ISO7816.OFFSET_CDATA
        // the PIN data length = byteRead
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false)
        {
            ISOException.throwIt(SW_VERIFICATION_FAILED);
        }

    } // end of validate method

    private void insertGradeHelper(byte[] buffer, short offset)
    {
        short studyFieldOffset = offset;
        short gradeFieldOffset = (short) (studyFieldOffset + 1);
        short dateFieldOffset = (short) (gradeFieldOffset + 1);
        
        switch(buffer[studyFieldOffset]) 
        {
            case firstStudyFieldCode:
            {
                firstStudyFieldGrade = buffer[gradeFieldOffset];
                if (firstStudyFieldDate == null)
                {
                    firstStudyFieldDate = new byte[10];
                }
                Util.arrayCopy(buffer, dateFieldOffset, firstStudyFieldDate, (short) 0, DATE_lENGTH);
                break;
            }
            case secondStudyFieldCode:
            {
                secondStudyFieldGrade = buffer[gradeFieldOffset];
                if (secondStudyFieldDate == null)
                {
                    secondStudyFieldDate = new byte[10];
                }
                Util.arrayCopy(buffer, dateFieldOffset, secondStudyFieldDate, (short) 0, DATE_lENGTH);
                break;
            }
            case thirdStudyFieldCode:
            {
                thirdStudyFieldGrade = buffer[gradeFieldOffset];
                if (thirdStudyFieldDate == null)
                {
                   thirdStudyFieldDate = new byte[10];
                }
                Util.arrayCopy(buffer, dateFieldOffset, thirdStudyFieldDate, (short) 0, DATE_lENGTH);
                break;
            }
            case fourthStudyFieldCode:
            {
                fourthStudyFieldGrade = buffer[gradeFieldOffset];
                if (fourthStudyFieldDate == null)
                {
                    fourthStudyFieldDate = new byte[10];
                }
                Util.arrayCopy(buffer, dateFieldOffset, fourthStudyFieldDate, (short) 0, DATE_lENGTH);
                break;
            }
            case fifthStudyFieldCode:
            {
                fifthStudyFieldGrade = buffer[gradeFieldOffset];
                if (fifthStudyFieldDate == null)
                {
                    fifthStudyFieldDate = new byte[10];
                }
                Util.arrayCopy(buffer, dateFieldOffset, fifthStudyFieldDate, (short) 0, DATE_lENGTH);
                break;
            }
            default:
            {
                break;
            }
        }
    }
    

    private void insertGrades(APDU apdu)
    {
        if (!pin.isValidated())
        {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        
        byte[] buffer = apdu.getBuffer();

        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        
        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if (numBytes != byteRead)
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        short offset = ISO7816.OFFSET_CDATA - 1;
        short payloadSize = buffer[offset];
        
        offset += 1;
        short numberOfInsertedGrades = buffer[offset];
        
        if (numberOfInsertedGrades > 5 || numberOfInsertedGrades < 1)
        {
            ISOException.throwIt(numberOfInsertedGrades);
        }
        
        //each gradePayload has the same size
        //studyFieldCode(1byte) + studyFieldGrade(1 byte) + studyFieldDate(10 bytes)
        short gradePayloadSize = (short) ((payloadSize - 1) / numberOfInsertedGrades);
        offset += 1;
        for (short index = 0; index < numberOfInsertedGrades; index++)
        {
            insertGradeHelper(buffer, offset);
            offset += gradePayloadSize;
        }
    }


    private void getGrades(APDU apdu)
    {
        if (!pin.isValidated())
        {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
        
        byte[] buffer = apdu.getBuffer();

        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        
        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if (numBytes != byteRead)
        {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
    }

} // end of class Wallet
