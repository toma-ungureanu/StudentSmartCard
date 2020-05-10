package database.test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import database.sources.Database;
import database.sources.StudentDatabaseRow;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatabaseTest
{
    @Test
    public void test_Modify_Grade_Smaller_Than_4() throws IOException
    {
        Database database = new Database(Database.TEST_DATABASE_PATH);
        StudentDatabaseRow student = new StudentDatabaseRow(1, 101);
        
        database.openDatabase();
        student = database.getStudentGradeInfo(student);
        student.setGrade(0);
        assertFalse(database.setStudentGrade(student));
        database.closeDatabase();
    }

    @Test
    public void test_Modify_Grade_Larger_Than_10() throws IOException
    {
        Database database = new Database(Database.TEST_DATABASE_PATH);
        StudentDatabaseRow student = new StudentDatabaseRow(1, 101);
        database.openDatabase();
        student = database.getStudentGradeInfo(student);
        student.setGrade(20);
        assertFalse(database.setStudentGrade(student));
        database.closeDatabase();
    }
    
    @Test
    public void test_Enter_New_Grade_Subject_Error() throws IOException
    {
        int studentId = 10;
        int subjectId = 111;
        int grade = 10;
        Date today = new Date();
        
        Database database = new Database(Database.TEST_DATABASE_PATH);
        StudentDatabaseRow student = new StudentDatabaseRow(studentId, subjectId);
        student.setGrade(grade);
        student.setGradeDate(today);
        
        database.openDatabase();
        assertFalse(database.setStudentGrade(student));
        database.closeDatabase();
    }
    
    @Test
    public void test_Enter_New_Grade_Success() throws IOException
    {
        for(int studentId = 1000; studentId < 1005; studentId++)
        {
            int subjectId = 101;
            int grade = 10;
            Date today = new Date();
            
            Database database = new Database(Database.TEST_DATABASE_PATH);
            StudentDatabaseRow student = new StudentDatabaseRow(studentId, subjectId);
            student.setGrade(grade);
            student.setGradeDate(today);
            
            database.openDatabase();
            assertTrue(database.setStudentGrade(student));
            database.closeDatabase();
        }
    }
    
    @Test
    public void test_Modify_Existing_Grade_Success() throws IOException
    {
        int studentId = 10;
        int subjectId = 101;
        int grade = 8;
        Date today = new Date();
        
        Database database = new Database(Database.TEST_DATABASE_PATH);
        StudentDatabaseRow student = new StudentDatabaseRow(studentId, subjectId);
        student.setGrade(grade);
        student.setGradeDate(today);
        
        database.openDatabase();
        database.setStudentGrade(student);
        database.closeDatabase();
        
        //modify the entry in the database with a new date and grade
        LocalDate finalDay = LocalDate.of(2020, Month.JULY, 9);  
        database.openDatabase();
        student = database.getStudentGradeInfo(student);
        student.setGradeDate(Date.from(finalDay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        student.setGrade(grade + 1);
        assertTrue(database.setStudentGrade(student));
        database.closeDatabase();
    }
    
}