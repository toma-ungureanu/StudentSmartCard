package terminal.sources;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadTransportException;
import database.sources.Database;
import database.sources.StudentDatabaseRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import static terminal.sources.Utils.*;

public class Terminal
{
    // code of CLA byte in the command APDU header
    final static byte STUDENT_CARD_CLA = (byte) 0x80;
    // codes of INS byte in the command APDU header
    final static byte GET_GRADE = (byte) 0x0A;
    final static byte GET_GRADES = (byte) 0x0B;
    final static byte GET_STUDENT_ID = (byte) 0x0C;
    final static byte INSERT_GRADE = (byte) 0x1A;
    final static byte INSERT_GRADES = (byte) 0x1B;
    final static byte VERIFY_PIN = (byte) 0x20;
    final static byte UPDATE_PIN = (byte) 0x70;
    final static short MAX_STUDENTS = 3000;

    private static final Database database = new Database();

    static void getGrades(CadClientInterface cad) throws IOException
    {
        System.out.println("Enter the study field id(s) separated by spaces");
        Scanner in = new Scanner(System.in);
        String studyFielsdStr = in.nextLine();
        String[] studyFieldsStrArray = studyFielsdStr.split(" ");
        ArrayList<Integer> studyFieldsArray = new ArrayList<>();
        for (String studyFieldStr : studyFieldsStrArray)
        {
            if (!isNumeric(studyFieldStr))
            {
                System.out.println("Incorrect study field entered: " + studyFieldStr);
                return;
            }

            int studyField = Integer.parseInt(studyFieldStr);
            if (!checkStudyFieldCode(studyField))
            {
                System.out.println("Study field unabailable");
                return;
            }
            studyFieldsArray.add(studyField);
        }

    }

    static void insertGrade() throws IOException
    {
        int studentId = promptStudentId();
        if (studentId == -1)
        {
            return;
        }

        int studyField = promptStudyField();
        if (studyField == -1)
        {
            return;
        }

        int grade = promptGrade();
        if (grade == -1)
        {
            return;
        }

        Date today = new Date();
        StudentDatabaseRow student = new StudentDatabaseRow();
        student.setGrade(grade);
        student.setStudentId(studentId);
        student.setSubjectId(studyField);
        student.setGradeDate(today);

        database.openDatabase();
        database.setStudentGrade(student);
        database.closeDatabase();

    }

    static void insertGrade(CadClientInterface cad) throws IOException
    {
        String PIN = promptForPin();
        verifyPIN(PIN, cad);

        int studyField = promptStudyField();
        if (studyField == -1)
        {
            return;
        }

        int grade = promptGrade();
        if (grade == -1)
        {
            return;
        }
        Date today = new Date();
        int studentId = getStudentId(cad);

        StudentDatabaseRow student = new StudentDatabaseRow(studentId, studyField);
        student.setGradeDate(today);
        student.setGrade(grade);

        Database database = new Database();
        database.openDatabase();
        database.setStudentGrade(student);
        database.closeDatabase();
    }

    static int getStudentId(CadClientInterface cad)
    {
        Apdu apdu = new Apdu();
        apdu.command = new byte[]{(byte) STUDENT_CARD_CLA, GET_STUDENT_ID, 0x00, 0x00};
        apdu.setDataIn(null, 0);
        apdu.setLe(0x02);

        System.out.println("command: " + apdu);
        try
        {
            cad.exchangeApdu(apdu);
        }
        catch (IOException | CadTransportException e)
        {
            e.printStackTrace();
        }

        System.out.println("response: " + apdu);
        byte[] response = apdu.getResponseApduBytes();
        return ((response[0] & 0xff) << 8) | (response[1] & 0xff);
    }

    static void debit(BufferedReader br, CadClientInterface cad, short amount) throws IOException, GeneralSecurityException
    {

    }

    static short getAmount(BufferedReader br) throws IOException
    {
        System.out.println("Insert amount: ");
        short amount = 0;
        try
        {
            amount = Short.parseShort(br.readLine());
        }
        catch (NumberFormatException e)
        {
            throw new IOException("Amount is invalid");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println(amount);
        return amount;
    }

    static String promptForPin() throws IOException
    {
        System.out.println("Transaction requires pin: ");
        String PIN = new Scanner(System.in).nextLine();

        for (int i = 0; i < PIN.length(); ++i)
        {
            if (PIN.charAt(i) < '0' || PIN.charAt(i) > '9')
            {
                System.out.println("Invalid PIN: " + PIN);
                break;
            }
        }
        return PIN;
    }

    static void verifyPIN(String pin, CadClientInterface cad)
    {
        byte[] PINBytes = new byte[pin.length()];
        for (int i = 0; i < pin.length(); ++i)
        {
            PINBytes[i] = (byte) (pin.charAt(i) - '0');
        }

        Apdu apdu = new Apdu();
        apdu.command = new byte[]{STUDENT_CARD_CLA, VERIFY_PIN, 0x00, 0x00};
        apdu.setDataIn(PINBytes, PINBytes.length);
        apdu.setLe(0x7f);

        System.out.println("command: " + apdu);
        try
        {
            cad.exchangeApdu(apdu);
        }
        catch (IOException | CadTransportException e)
        {
            e.printStackTrace();
        }

        System.out.println("response: " + apdu);
    }


    static void getBalance(CadClientInterface cad)
    {
        Apdu apdu = new Apdu();
        apdu.command = new byte[]{(byte) 0x80, 0x50, 0x00, 0x00};
        apdu.setDataIn(null, 0);
        apdu.setLe(0x02);

        System.out.println("command: " + apdu);
        try
        {
            cad.exchangeApdu(apdu);
        }
        catch (IOException | CadTransportException e)
        {
            e.printStackTrace();
        }

        System.out.println("response: " + apdu);

    }
}
