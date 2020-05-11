package terminal.sources;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadTransportException;
import database.sources.Database;
import database.sources.StudentDatabaseRow;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import static terminal.sources.Utils.*;

public class Terminal
{
    // code of CLA byte in the command APDU header
    final static byte STUDENT_CARD_CLA = (byte) 0x80;
    // codes of INS byte in the command APDU header
    final static byte GET_GRADES = (byte) 0x0B;
    final static short GRADE_PAYLOAD_SIZE = 12;
    final static byte GET_STUDENT_ID = (byte) 0x0C;
    final static byte INSERT_GRADES = (byte) 0x1B;
    final static byte VERIFY_PIN = (byte) 0x20;
    final static short MAX_STUDENTS = 3000;

    private static final Database database = new Database();

    static void getGrades(CadClientInterface cad) throws IOException
    {
        String PIN = promptPin();
        verifyPIN(PIN, cad);
        ArrayList<Short> studyFieldsArray = promptStudyFields();
        if (studyFieldsArray == null)
        {
            return;
        }
        Apdu apdu = new Apdu();
        apdu.command = new byte[]{STUDENT_CARD_CLA, GET_GRADES, 0x00, 0x00};
        byte[] inputData = new byte[studyFieldsArray.size()];
        for (int index = 0; index < inputData.length; index++)
        {
            inputData[index] = studyFieldsArray.get(index).byteValue();
        }
        apdu.setDataIn(inputData, inputData.length);
        short leValue = (short) (studyFieldsArray.size() * GRADE_PAYLOAD_SIZE);
        apdu.setLe((byte) leValue);

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

    static void payTax(CadClientInterface cad) throws IOException
    {
        int studentId = getStudentId(cad);
        database.openDatabase();
        ArrayList<StudentDatabaseRow> gradeList = database.getStudentGrades(studentId);
        database.closeDatabase();

        ArrayList<Integer> subjectTaxes = new ArrayList<>();
        for (StudentDatabaseRow studentDatabaseRow : gradeList)
        {
            if (!studentDatabaseRow.getIsGradeValid() && !studentDatabaseRow.getIsTaxPayed())
            {
                System.out.println("Tax needed for: " + studentDatabaseRow.getSubjectName() + "(" + studentDatabaseRow.getSubjectId() + ")");
                subjectTaxes.add(studentDatabaseRow.getSubjectId());
            }
        }

        if (subjectTaxes.size() == 0)
        {
            System.out.println("No tax to pay!");
        }

        else
        {
            System.out.println("You can pay taxes for " + subjectTaxes.size() + " subjects");
            ArrayList<Short> studyFields = promptStudyFields();
            if (studyFields == null)
            {
                return;
            }

            database.openDatabase();
            for(Short studyfield: studyFields)
            {
                for(StudentDatabaseRow studentDatabaseRow: gradeList)
                {
                    if(studentDatabaseRow.getSubjectId() == studyfield)
                    {
                        studentDatabaseRow.setIsTaxPayed(true);
                        database.payTax(studentDatabaseRow);
                        break;
                    }
                }
            }
            database.closeDatabase();
        }
    }

    static void syncDatabaseAndCard(CadClientInterface cad) throws IOException
    {
        String PIN = promptPin();
        verifyPIN(PIN, cad);

        int studentId = getStudentId(cad);
        database.openDatabase();
        ArrayList<StudentDatabaseRow> gradesList = database.getStudentGrades(studentId);
        database.closeDatabase();
        System.out.println("########## STUDENT GRADES ##########");
        for (StudentDatabaseRow studentDatabaseRow : gradesList)
        {
            if(studentDatabaseRow.getIsGradeValid())
            {
                insertGradeInCard(cad, studentDatabaseRow);
            }
            else
            {
                System.out.println("Cannot insert grade in card. Grade not valid!");
                System.out.println(studentDatabaseRow.getSubjectName()+"(" + studentDatabaseRow.getSubjectId() + ")");
            }
            System.out.println("##########");
            fancyPrint(studentDatabaseRow);
            System.out.println("##########");
            System.out.println();
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
        student.setStudentId(studentId);
        student.setSubjectId(studyField);

        database.openDatabase();
        student = database.getStudentGradeInfo(student);
        student.setGrade(grade);
        student.setGradeDate(today);
        database.setStudentGrade(student);
        database.closeDatabase();

    }

    static void insertGrade(CadClientInterface cad) throws IOException
    {
        String PIN = promptPin();
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

        insertGradeInCard(cad, student);
        Database database = new Database();
        database.openDatabase();
        database.setStudentGrade(student);
        database.closeDatabase();
    }

    private static void insertGradeInCard(CadClientInterface cad, StudentDatabaseRow student)
    {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        if (student.getStudentId() > MAX_STUDENTS || student.getStudentId() < 0)
        {
            System.out.println("Wrong studentId");
            return;
        }

        byte[] payload = new byte[GRADE_PAYLOAD_SIZE + 1]; // + 1 because we introduce one grade
        payload[0] = (byte) 1;
        payload[1] = (byte) student.getSubjectId();
        payload[2] = (byte) student.getGrade();
        System.arraycopy(dateFormat.format(student.getGradeDate()).getBytes(), 0, payload, 3, 10);

        Apdu apdu = new Apdu();
        apdu.command = new byte[]{STUDENT_CARD_CLA, INSERT_GRADES, 0x00, 0x00};
        apdu.setDataIn(payload, payload.length);
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

    static int getStudentId(CadClientInterface cad)
    {
        Apdu apdu = new Apdu();
        apdu.command = new byte[]{STUDENT_CARD_CLA, GET_STUDENT_ID, 0x00, 0x00};
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
        byte[] response = apdu.dataOut;
        return ((response[0] & 0xff) << 8) | (response[1] & 0xff);
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
}
