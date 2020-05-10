import java.util.Date;

public class StudentDatabaseRow
{
    private int m_studentId;
    private int m_grade;
    private boolean m_isTaxPayed;
    private int m_subjectId;
    private String m_subjectName;
    private Date m_gradeDate;
    private int m_numberOfReexams;
    private boolean m_isGradeValid;

    public StudentDatabaseRow(int studentId, int subjectId, String subjectName, Date gradeDate, int grade, boolean isTaxNeeded,
            int numberOfReexams, boolean isGradeValid)
    {
        this.m_studentId = studentId;
        this.m_subjectId = subjectId;
        this.m_subjectName = subjectName;
        this.m_gradeDate = gradeDate;
        this.m_grade = grade;
        this.m_isTaxPayed = isTaxNeeded;
        this.m_numberOfReexams = numberOfReexams;
        this.m_isGradeValid = isGradeValid;
    }
    
    public StudentDatabaseRow(int studentId, int subjectId)
    {
        this.m_studentId = studentId;
        this.m_subjectId = subjectId;
    }

    public boolean getIsTaxPayed()
    {
        return m_isTaxPayed;
    }

    public void setIsTaxPayed(boolean isTaxPayed)
    {
        this.m_isTaxPayed = isTaxPayed;
    }

    public int getNumberOfReexams()
    {
        return m_numberOfReexams;
    }

    public void setNumberOfReexams(int numberOfReexams)
    {
        this.m_numberOfReexams = numberOfReexams;
    }

    public boolean getIsGradeValid()
    {
        return m_isGradeValid;
    }

    public int getStudentId()
    {
        return m_studentId;
    }

    public void setStudentId(int studentId)
    {
        this.m_studentId = studentId;
    }

    public int getGrade()
    {
        return m_grade;
    }

    public void setGrade(int grade)
    {
        this.m_grade = grade;
    }

    public boolean isTaxNeeded()
    {
        return m_isTaxPayed;
    }

    public void setTaxNeeded(boolean isTaxNeeded)
    {
        this.m_isTaxPayed = isTaxNeeded;
    }

    public int getSubjectId()
    {
        return m_subjectId;
    }

    public void setSubjectId(int subjectId)
    {
        this.m_subjectId = subjectId;
    }

    public String getSubjectName()
    {
        return m_subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.m_subjectName = subjectName;
    }

    public Date getGradeDate()
    {
        return m_gradeDate;
    }

    public void setGradeDate(Date gradeDate)
    {
        this.m_gradeDate = gradeDate;
    }
}
