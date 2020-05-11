package terminal.sources;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadTransportException;
import database.sources.StudentDatabaseRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static terminal.sources.Terminal.MAX_STUDENTS;

public class Utils
{
    static void runScript(CadClientInterface cad, String filePath)
    {
        try (Stream<String> stream = Files.lines(Paths.get(filePath)))
        {
            stream
                    .filter(s ->
                    {
                        if (s.isEmpty())
                        {
                            return false;
                        }
                        if (Character.isLetter(s.charAt(0)))
                        {
                            return false;
                        }
                        return !s.startsWith("/");
                    })
                    .map(line -> Arrays.stream(line.replace(";", "").split(" ")).map(x ->
                    {
                        char first = x.charAt(2);
                        char second = x.charAt(3);

                        return (byte) (((Integer.parseInt(String.valueOf(first), 16)) * 16) + Integer.parseInt(String.valueOf(second), 16));
                    })
                            .collect(Collectors.toList()))
                    .forEach(bytes ->
                    {
                        Apdu apdu = new Apdu();
                        apdu.command = new byte[]{bytes.get(0), bytes.get(1), bytes.get(2), bytes.get(3)};
                        byte[] data = new byte[bytes.size() - 6];
                        for (int i = 5; i < bytes.size() - 1; i++)
                        {
                            data[i - 5] = bytes.get(i);
                        }
                        apdu.setDataIn(data, bytes.get(4));
                        apdu.setLe(bytes.get(bytes.size() - 1));
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
                    });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static boolean isNumeric(String strNum)
    {
        if (strNum == null)
        {
            return false;
        }
        try
        {
            Integer integer = Integer.parseInt(strNum);
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }


    static boolean checkStudyFieldCode(int studyField)
    {
        if (studyField < 101 || studyField > 105)
        {
            System.out.println("Study field unavailable");
            return false;
        }
        return true;
    }

    static boolean checkStudentId(int studentId)
    {
        return studentId > 0 && studentId < 3000;
    }

    static int promptStudyField()
    {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter the study field code: ");
        String studyFieldStr = in.nextLine();
        if (!isNumeric(studyFieldStr))
        {
            System.out.println("Invalid Study field");
            return -1;
        }

        int studyField = Integer.parseInt(studyFieldStr);
        if (studyField < 101 || studyField > 105)
        {
            System.out.println("Invalid study field selected: " + studyField);
            return -1;
        }
        return studyField;
    }

    static int promptGrade()
    {
        System.out.println("Enter the grade(between 4 and 10): ");
        Scanner in = new Scanner(System.in);
        String gradeStr = in.nextLine();
        if (!isNumeric(gradeStr))
        {
            System.out.println("Invalid grade");
            return -1;
        }

        int grade = Integer.parseInt(gradeStr);
        if (grade < 4 || grade > 10)
        {
            System.out.println("Incorrect grade inserted");
            return -1;
        }
        return grade;
    }

    static int promptStudentId()
    {
        System.out.println("Enter the student's id: ");
        Scanner in = new Scanner(System.in);
        String gradeStr = in.nextLine();
        if (!isNumeric(gradeStr))
        {
            System.out.println("Invalid grade");
            return -1;
        }

        int studentId = Integer.parseInt(gradeStr);
        if (studentId > MAX_STUDENTS)
        {
            System.out.println("Wrong student ID!");
            return -1;
        }
        return studentId;
    }

    static String promptPin() throws IOException
    {
        System.out.println("Enter your PIN:");
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

    static void fancyPrint(StudentDatabaseRow studentDatabaseRow)
    {
        System.out.println("####    Subject: " + studentDatabaseRow.getSubjectName() + "(" + studentDatabaseRow.getSubjectId() + ")");
        System.out.println("####    Grade: " + studentDatabaseRow.getGrade());
        System.out.println("####    Date: " + studentDatabaseRow.getGradeDate());
        System.out.println("####    Valid Grade: " + studentDatabaseRow.getIsGradeValid());
        if (studentDatabaseRow.getGrade() < 5)
        {
            System.out.println("####    Reexaminations: " + studentDatabaseRow.getNumberOfReexams());
            System.out.println("####    Tax: " + studentDatabaseRow.getIsTaxPayed());
        }
    }


    static ArrayList<Short> promptStudyFields()
    {
        System.out.println("Enter the study field id(s) separated by spaces");
        Scanner in = new Scanner(System.in);
        String studyFielsdStr = in.nextLine();
        String[] studyFieldsStrArray = studyFielsdStr.split(" ");
        ArrayList<Short> studyFieldsArray = new ArrayList<>();
        for (String studyFieldStr : studyFieldsStrArray)
        {
            if (!isNumeric(studyFieldStr))
            {
                System.out.println("Incorrect study field entered: " + studyFieldStr);
                return null;
            }

            short studyField = Short.parseShort(studyFieldStr);
            if (!checkStudyFieldCode(studyField))
            {
                System.out.println("Study field unavailable");
                return null;
            }
            studyFieldsArray.add(studyField);
        }
        return studyFieldsArray;
    }

}
