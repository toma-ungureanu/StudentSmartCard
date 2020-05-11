package database.sources;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Database
{
    public static final String DATABASE_PATH = "C:\\Users\\Tomy\\eclipse-workspace\\StudentCardProject\\Terminal\\src\\database\\resources\\students.xlsx";
    public static final String TEST_DATABASE_PATH = "C:\\Users\\Tomy\\eclipse-workspace\\StudentCardProject\\Terminal\\src\\database\\resources\\students-Copy.xlsx";

    private File m_databaseFile = new File(DATABASE_PATH);
    private static final int STUDENT_ID_FIELD = 0;
    private static final int SUBJECT_ID_FIELD = 1;
    private static final int SUBJECT_NAME_FIELD = 2;
    private static final int GRADE_DATE_FIELD = 3;
    private static final int GRADE_FIELD = 4;
    private static final int IS_TAX_PAYED_FIELD = 5;
    private static final int NUMBER_OF_REEXAMS_FIELD = 6;
    private static final int IS_GRADE_VALID_FIELD = 7;
    private XSSFWorkbook m_book;
    private XSSFSheet m_sheet;
    private boolean m_isDatabaseOpen = false;

    public Database()
    {
    }

    public Database(String dataBasePath)
    {
        m_databaseFile = new File(dataBasePath);
    }

    public synchronized void closeDatabase() throws IOException
    {
        if (m_isDatabaseOpen)
        {
            m_book.close();
            System.out.println("Database closed!");
            m_isDatabaseOpen = false;
        }
    }

    public synchronized void openDatabase() throws IOException
    {
        if (!m_isDatabaseOpen)
        {
            FileInputStream m_fis = new FileInputStream(m_databaseFile);
            m_book = new XSSFWorkbook(m_fis);
            m_sheet = m_book.getSheetAt(0);
            m_isDatabaseOpen = true;
            System.out.println("Database open!");
        }
    }

    private StudentDatabaseRow constructStudentObject(Row row)
    {
        return new StudentDatabaseRow((int) row.getCell(STUDENT_ID_FIELD).getNumericCellValue(),
                (int) row.getCell(SUBJECT_ID_FIELD).getNumericCellValue(),
                row.getCell(SUBJECT_NAME_FIELD).getStringCellValue(), row.getCell(GRADE_DATE_FIELD).getDateCellValue(),
                (int) row.getCell(GRADE_FIELD).getNumericCellValue(),
                row.getCell(IS_TAX_PAYED_FIELD).getBooleanCellValue(),
                (int) row.getCell(NUMBER_OF_REEXAMS_FIELD).getNumericCellValue(),
                row.getCell(IS_GRADE_VALID_FIELD).getBooleanCellValue());
    }

    public ArrayList<StudentDatabaseRow> getStudentGrades(int studentId)
    {
        ArrayList<StudentDatabaseRow> gradesList = new ArrayList<>();
        int index = 0;
        for (Row row : m_sheet)
        {
            if (index == 0)
            {
                index++;
                continue;
            }

            if (row.getCell(STUDENT_ID_FIELD) != null)
            {
                int rowStudentId = (int) row.getCell(STUDENT_ID_FIELD).getNumericCellValue();
                if (rowStudentId == studentId)
                {
                    gradesList.add(constructStudentObject(row));
                }
            }
        }
        return gradesList;
    }

    private String getSubjectName(int subjectId)
    {
        switch (subjectId)
        {
            case 101:
                return "Matematica";
            case 102:
                return "Fundamente algebrice ale informaticii";
            case 103:
                return "Securitatea informatiei";
            case 104:
                return "Smart cards si aplicatii";
            case 105:
                return "Introducere in criptografie";
            default:
                return null;
        }
    }

    public List<StudentDatabaseRow> getSheet() throws IOException
    {
        List<StudentDatabaseRow> studentDatabaseRows = new ArrayList<>();
        for (Row row : m_sheet)
        {
            studentDatabaseRows.add(constructStudentObject(row));
        }

        return studentDatabaseRows;
    }

    public StudentDatabaseRow getStudentGradeInfo(StudentDatabaseRow studentDatabaseRow) throws IOException
    {
        int index = 0;
        for (Row row : m_sheet)
        {
            if (index == 0)
            {
                index++;
                continue;
            }

            if (row.getCell(STUDENT_ID_FIELD) != null && row.getCell(SUBJECT_ID_FIELD) != null)
            {
                int studentId = (int) row.getCell(STUDENT_ID_FIELD).getNumericCellValue();
                int subjectId = (int) row.getCell(SUBJECT_ID_FIELD).getNumericCellValue();

                if (studentId == studentDatabaseRow.getStudentId() && subjectId == studentDatabaseRow.getSubjectId())
                {
                    return constructStudentObject(row);
                }
            }
        }

        return null;
    }

    private boolean createCellStyle(Cell cell, int rowNum, String format, int field, StudentDatabaseRow studentDatabaseRow)
    {
        CellStyle cellStyle = m_book.createCellStyle();
        CreationHelper createHelper = m_book.getCreationHelper();

        if ("STRING".equals(format))
        {
            cell.setCellValue(studentDatabaseRow.getSubjectName());
        }
        else if ("NUMBER".equals(format))
        {
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("0"));
            switch (field)
            {
                case GRADE_FIELD:
                {
                    int grade = studentDatabaseRow.getGrade();
                    if (grade < 4 || grade > 10)
                    {
                        System.out.println("Invalid grade entered!");
                        return false;
                    }
                    cell.setCellValue(studentDatabaseRow.getGrade());
                    break;
                }
                case NUMBER_OF_REEXAMS_FIELD:
                {
                    cell.setCellValue(studentDatabaseRow.getNumberOfReexams());
                    break;
                }
                case SUBJECT_ID_FIELD:
                {
                    cell.setCellValue(studentDatabaseRow.getSubjectId());
                    break;
                }
                case STUDENT_ID_FIELD:
                {
                    cell.setCellValue(studentDatabaseRow.getStudentId());
                    break;
                }
                default:
                    System.out.println("Unavailable field!");
                    return false;
            }
        }
        else if ("DATE".equals(format))
        {
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));
            cell.setCellValue(studentDatabaseRow.getGradeDate());
        }
        else if ("BOOLEAN".equals(format))
        {
            if (field == IS_TAX_PAYED_FIELD)
            {
                cell.setCellValue(studentDatabaseRow.getIsTaxPayed());
            }
            else if (field == IS_GRADE_VALID_FIELD)
            {
                String formula = "IF(OR(E" + rowNum + ">= 5,AND(E" + rowNum +
                        " < 5, F" + rowNum + " = TRUE, G" + rowNum + " >= 1)),TRUE,FALSE)";

                cell.setCellFormula(formula);
                FormulaEvaluator evaluator = m_book.getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                cell.setCellValue(cellValue.getBooleanValue());
            }
        }

        cell.setCellStyle(cellStyle);
        return true;
    }

    private boolean insertNewGrade(StudentDatabaseRow studentDatabaseRow)
    {
        int numberOfNonEmptyRows = 0;
        for (Row row : m_sheet)
        {
            if (row.getCell(STUDENT_ID_FIELD) != null && row.getCell(STUDENT_ID_FIELD).getCellType() != CellType.BLANK)
            {
                numberOfNonEmptyRows++;
            }
            else
            {
                break;
            }
        }

        Row row = m_sheet.createRow(numberOfNonEmptyRows);
        String subjectName = getSubjectName(studentDatabaseRow.getSubjectId());
        if (subjectName == null)
        {
            System.out.println("Non existent subject!");
            return false;
        }
        studentDatabaseRow.setSubjectName(subjectName);

        int rowNum = row.getRowNum() + 1;
        Cell cell = row.createCell(STUDENT_ID_FIELD);
        createCellStyle(cell, rowNum, "NUMBER", STUDENT_ID_FIELD, studentDatabaseRow);

        cell = row.createCell(SUBJECT_ID_FIELD);
        createCellStyle(cell, rowNum, "NUMBER", SUBJECT_ID_FIELD, studentDatabaseRow);

        cell = row.createCell(SUBJECT_NAME_FIELD);
        createCellStyle(cell, rowNum, "STRING", SUBJECT_NAME_FIELD, studentDatabaseRow);

        cell = row.createCell(GRADE_FIELD);
        createCellStyle(cell, rowNum, "NUMBER", GRADE_FIELD, studentDatabaseRow);

        cell = row.createCell(GRADE_DATE_FIELD);
        createCellStyle(cell, rowNum, "DATE", GRADE_DATE_FIELD, studentDatabaseRow);

        cell = row.createCell(IS_TAX_PAYED_FIELD);
        createCellStyle(cell, rowNum, "BOOLEAN", IS_TAX_PAYED_FIELD, studentDatabaseRow);

        cell = row.createCell(NUMBER_OF_REEXAMS_FIELD);
        createCellStyle(cell, rowNum, "NUMBER", NUMBER_OF_REEXAMS_FIELD, studentDatabaseRow);

        cell = row.createCell(IS_GRADE_VALID_FIELD);
        createCellStyle(cell, rowNum, "BOOLEAN", IS_GRADE_VALID_FIELD, studentDatabaseRow);

        return true;
    }

    private boolean modifyStudentGrade(Row row, StudentDatabaseRow student) throws IOException
    {
        int newGrade = student.getGrade();
        int currentGrade = (int) row.getCell(GRADE_FIELD).getNumericCellValue();
        int rowIndex = row.getRowNum() + 1;
        if (currentGrade < newGrade)
        {
            createCellStyle(row.getCell(GRADE_FIELD), rowIndex, "NUMBER", GRADE_FIELD, student);
            createCellStyle(row.getCell(GRADE_DATE_FIELD), rowIndex, "DATE", GRADE_DATE_FIELD, student);
            createCellStyle(row.getCell(IS_GRADE_VALID_FIELD), rowIndex, "BOOLEAN", IS_GRADE_VALID_FIELD, student);
        }

        student.setNumberOfReexams(student.getNumberOfReexams() + 1);
        createCellStyle(row.getCell(NUMBER_OF_REEXAMS_FIELD), rowIndex, "NUMBER", NUMBER_OF_REEXAMS_FIELD, student);
        return true;
    }

    public boolean payTax(StudentDatabaseRow studentDatabaseRow) throws IOException
    {
        int index = 0;
        for (Row row : m_sheet)
        {
            if (index == 0)
            {
                index++;
                continue;
            }

            if ((int) row.getCell(STUDENT_ID_FIELD).getNumericCellValue() == studentDatabaseRow.getStudentId()
                    && (int) row.getCell(SUBJECT_ID_FIELD).getNumericCellValue() == studentDatabaseRow.getSubjectId())
            {
                int rowNum = row.getRowNum() + 1;
                createCellStyle(row.getCell(IS_TAX_PAYED_FIELD), rowNum, "BOOLEAN", IS_TAX_PAYED_FIELD, studentDatabaseRow);
                studentDatabaseRow.setIsGradeValid(true);
                createCellStyle(row.getCell(IS_GRADE_VALID_FIELD), rowNum, "BOOLEAN", IS_GRADE_VALID_FIELD, studentDatabaseRow);
                System.out.println("Tax payed succesfully for: " + studentDatabaseRow.getSubjectName() + "(" + studentDatabaseRow.getSubjectId() + ")");
                FileOutputStream outputStream = new FileOutputStream(m_databaseFile);
                m_book.write(outputStream);
                break;
            }
        }
        return true;
    }

    public boolean setStudentGrade(StudentDatabaseRow studentDatabaseRow) throws IOException
    {
        int studentGrade = studentDatabaseRow.getGrade();
        if (studentGrade < 4 || studentGrade > 10)
        {
            System.out.println("Wrong grade inserted");
            return false;
        }

        int studyField = studentDatabaseRow.getSubjectId();
        if (studyField < 101 || studyField > 105)
        {
            System.out.println("Wrong study field id inserted");
            return false;
        }

        boolean found = false;
        boolean retVal = true;
        int index = 0;
        for (Row row : m_sheet)
        {
            if (index == 0)
            {
                index++;
                continue;
            }

            if (row.getCell(STUDENT_ID_FIELD) != null && row.getCell(SUBJECT_ID_FIELD) != null)
            {
                int studentId = (int) row.getCell(STUDENT_ID_FIELD).getNumericCellValue();
                int subjectId = (int) row.getCell(SUBJECT_ID_FIELD).getNumericCellValue();
                if (studentId == studentDatabaseRow.getStudentId() && subjectId == studentDatabaseRow.getSubjectId())
                {
                    found = true;
                    if (!modifyStudentGrade(row, studentDatabaseRow))
                    {
                        retVal = false;
                    }
                    break;
                }
            }
            else
            {
                break;
            }
        }

        if (!found)
        {
            retVal = insertNewGrade(studentDatabaseRow);
        }

        if (retVal)
        {
            FileOutputStream outputStream = new FileOutputStream(m_databaseFile);
            m_book.write(outputStream);
        }
        return retVal;
    }

}
