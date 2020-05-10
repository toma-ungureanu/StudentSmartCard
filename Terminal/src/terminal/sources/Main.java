package terminal.sources;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;
import database.sources.Database;
import database.sources.StudentDatabaseRow;

import static terminal.sources.Utils.*;

public class Main
{
    public static String installPath = "C:\\Users\\Tomy\\eclipse-workspace\\card\\apdu_scripts\\cap-card.script";
    public static String initPath = "C:\\Users\\Tomy\\eclipse-workspace\\card\\apdu_scripts\\init.scr";

    private static final Database database = new Database();

    public static void main(String[] params)
    {
        try
        {
            CadClientInterface cad;
            Socket sock;

            sock = new Socket("localhost", 9025);
            InputStream is = sock.getInputStream();
            OutputStream os = sock.getOutputStream();

            cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
            System.out.println("Powering up...");
            cad.powerUp();
            System.out.println("Powered up!");

            runScript(cad, installPath);
            System.out.println("Installing ");
            runScript(cad, initPath);

            label_1:
            while (true)
            {
                try
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                    System.out.println("Insert method(1/2/3/4): ");
                    System.out.println("1. Exam grade(student card present)");
                    System.out.println("2. Exam grade(based on given student id)");
                    System.out.println("3. Get card grade(s)");
                    System.out.println("4. Sync database and student card");
                    System.out.println("5. Close");
                    String method = br.readLine().toLowerCase();

                    if (!isNumeric(method))
                    {
                        System.out.println("Method not recognized");
                        return;
                    }

                    switch (Integer.parseInt(method))
                    {
                        case 1:
                            Terminal.insertGrade(cad);
                            break;

                        case 2:
                            Terminal.insertGrade();
                            break;

                        case 3:
                            break;

                        case 4:
                            break;

                        case 5:
                            cad.powerDown();
                            sock.close();

                            break label_1;

                        default:
                            System.out.println("Invalid command: " + method);
                    }


                }
                catch (IOException | IllegalArgumentException e)
                {
                    System.out.println(e.getMessage());
                }

            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



}
