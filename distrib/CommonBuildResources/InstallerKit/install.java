/**
        $Id$

        Here we define the support for installing jape (or any other
        application) on any machine on which java is installed. It
        was designed and written in less than a working day, so
        there may be some aspects of its implementation to simplify,
        and some aspects of its functionality to enhance, but for
        the moment it will do the job we need doing.

        Our method is to build an (executable) jar file containing
        everything that the application needs, together with a
        primary bootstrap installer class whose main program gets
        executed from the jar.

        The primary bootstrap unpacks the jar in-situ at the
        installation site, before loading and instantiating a
        designated secondary bootstrap that can be written in
        java, or (on Unix machines) can be a shell script.

        The point of building an executable jar is that some Windows
        java installations will let one execute a jar file by
        double-clicking on it -- this saves the poor installer from
        having to find out how to get java to work on a command line,
        and/or finding a way of unpacking a jar.

        TO USE
        ------

        Assemble all the components you need for your application to work
        in a directory $(BUILD) 
        
                mkdir $(BUILD)/bootstrap
                cp    install.class       $(BUILD)/bootstrap
                cd    $(BUILD)
                java  bootstrap.install   [switches] $(TARGET).jar $(RESOURCEFILENAMES)

        This builds a jar called $(TARGET).jar that can be transported anywhere.

        It is fairly important that the name of this file is not
        changed, for if it is then the installation process is made a
        little more complicated (see below). We could make it
        simpler, but (frankly) it isn't worth it.
        
        Switches are:

         -splash        <image file>        -- specify the installation splash image
         -app           "the app name"      -- default is Jape
         -bfont         fontname            -- button font            (default Sanserif-PLAIN-14)
         -tfont         fontname            -- feedback window font   (default Monospaced-PLAIN-14)
         -cmdunix       "command"           -- specify the post-unpack shell command for Unixoid systems
         -cmdwindows    "command"           -- specify the post-unpack shell command for Windoid systems
         -classwindows  <class name>        -- specify the post-unpack class to load and instantiate (on Windows)
         -classunix     <class name>        -- specify the post-unpack class to load and instantiate (on non-Windows)

        (The classes specified by -classXXX are loaded and instantiated before the scripts/commands
         specified by -cmdXXX are run)

       Switches that modify the way resource files are treated
         -boot                              -- synchronize the named files which follow into the installation bootstrap directory before making the jar
         +boot                              -- turn off -boot
         -sync                              -- install the files which follow as if they came from the directory
                                               in which the installation is being constructed.
         +sync                              -- turns off -sync

        WINDOWS/APPLE INSTALL
        ---------------------

        To install on a Windows (or, I suppose, a Napple) machine,
        on which java has been properly installed (only the jre
        is needed, not the whole jdk) just place $(TARGET).jar in
        the installation folder you have chosen, then doubleclick
        on it. A control panel appears which gives you the option
        to install the software. The name of the expected target
        jar file is displayed: it should be the same as the name
        of the jar file that was doubleclicked. If not, then the
        install process will not succeed.

        If $(TARGET).jar file has changed its name since it was
        made, then the simplest way to recover is to change it back
        to the original and double click on it as above.

        It's usually possible to arrange for a combination of the
        -cmdwindows and -classwindows switches to create runtime scripts,
        and to make shortcuts to them with the appropriate icons
        within. The resource SHORTCUT.EXE (a Win32 program) should
        be shipped to do the latter job. Here's how it might be used:

        shortcut.exe 
          -t target file
          -a arguments
          -d directory to start in
          -i iconfile
          -x index into the iconfile
          -n name of the shortcut
          -f force overwrite of existing shortcut
          -u [natdix or all] show the content of an existing shortcut

        example
           shortcut.exe -f -t C:\japehome\jape.exe -d C:\japehome -n %userprofile%"\start menu\programs\jape" -i japeicon.ico
          
        
        UNIXOID INSTALL
        ---------------

        To install on a Unixoid machine, on which java has been
        properly installed, (only the jre is needed, not the whole
        jdk), just place $(TARGET).jar in the installation folder
        you have chosen, then
        
                java -jar $(TARGET).jar

        A control panel appears which gives you the option
        to install the software. The name of the expected target
        jar file is displayed: it should be the same as the name
        of the jar file that was executed. If not, then the
        install process will not succeed.

        If the $(TARGET).jar file has changed its name (say to
        foo.jar) since it was made, then
        
                java -jar foo.jar foo.jar

        will do the trick. 
                

        TO DO
        -----

        It would be simpler and better if this class were split into
        the bootstrap-builder, and the primary bootstrap installer, but
        (at the moment) the overhead of the builder being present at
        install time is rather small (about 10kb). 

        I may need to add something for MacOSX installation.

        
        
*/

/*

 This program is free software; you can redistribute it and/or modify
 it under the terms of the  GNU General Public License  as published
 by the Free Software Foundation; either version 2 of the License,
 or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

*/

package bootstrap;

// FOR THE INSTALL-TIME GUI
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// FOR JAR UNPACKING
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class install implements ActionListener
{ static String 
         packageName        = "bootstrap"
  ,      packagePath        = "bootstrap"
  ,      installerName      = "install"
  ,      installerClass     = packageName+"."+installerName
  ,      manifestName       = packagePath+"/"+installerName + ".mf"
  ,      SplashImage        = null
  ,      propertiesResource = installerName+".properties"
  ,      propertiesFile     = packagePath+"/"+propertiesResource
  ;

  /////////////////////////// CONFIGURE TIME ////////////////////////////
  
  public static void main(String[] args) throws Exception
  { 
    if (args.length>1)
      makeInstaller(args);
    else
      new install(args.length==1?args[0]:null);
  }

  private static String withoutPath(String s)
  { return new File(s).getName(); }

  public static void makeInstaller(String[] args) throws Exception
  { 
    String       installjar    = null;
    StringBuffer resources     = new StringBuffer();
    StringBuffer syncresources = new StringBuffer();
    StringBuffer bootresources = new StringBuffer();
    Properties   props         = new Properties();
    boolean      sync          = false;
    boolean      boot          = false;
    
    for (int i=0; i<args.length; i++)
    {
       String arg=args[i];
       if (arg.equals("-sync")) { sync=true; boot=false; }
       else
       if (arg.equals("+sync")) { sync=false; }
       else
       if (arg.equals("-boot")) { boot=true; sync=false; }
       else
       if (arg.equals("+boot")) { boot=false; }
       else
       if (arg.startsWith("-"))
       { String param = args[++i];
         if (arg.startsWith("-C"))
            resources.append(" -C " + withoutPath(param));
         else
         if (arg.startsWith("-splash")) 
         { SplashImage = param; 
           bootresources.append(" " + SplashImage);
           props.setProperty("-splash", withoutPath(SplashImage));
         }
         else
         { 
           props.setProperty(arg, param);
           if (arg.startsWith("-cmdwindows") && param.endsWith(".cmd"))
           { 
             resources.append(" " + withoutPath(param));
             if (sync) syncresources.append(" "+param);
           }
         }
       }
       else
       if (installjar==null && arg.endsWith(".jar")) 
       {
          installjar = arg;
          props.setProperty("-jar", installjar);
       }
       else
       {
          if (sync) syncresources.append(" "+arg);
          
          if (boot) 
             bootresources.append(" "+arg);
          else
             resources.append(" "+withoutPath(arg));
       }
    }
      
    FileOutputStream propfile = new FileOutputStream(propertiesFile);
    props.store(propfile, "Installer properties");
    propfile.close();
 
    if ( installjar == null )
    {
       System.err.println("Usage: java bootstrap.install [-splash imagefile] target.jar [resource]*");  
       System.exit(1);
    }
    else
    {  
       System.err.println("[Bootstrap installer building  "+installjar +"]");  
              
       String[] manifest =
       { "Main-Class: "+installerClass
       };
       cat(manifestName,    manifest);
       
       boolean ok = execute
       ( new String[]
         { syncresources.toString().equals("")?"#no resources to sync":"rsync -v -t "+syncresources.toString()+" ."
         , bootresources.toString().equals("")?"#no install-time-only resources to sync":"rsync -v -t "+bootresources.toString()+" "+packagePath
         , "jar -cvfm " + installjar + " " + manifestName + " " + packagePath + " " + resources.toString()
         },
         false
       );
 
       if (!ok) 
       { System.err.println("[Failed to build the installer]");
         System.exit(1);
       }
       else
         System.err.println("[Bootstrap installer completed  "+installjar +"]");  
    }
  }

  public static String quote(String s) { return s==null?null:"\""+s+"\""; }

  public static void cat(String filename, String[] lines) throws Exception
  {
       PrintWriter out = new PrintWriter(new FileOutputStream(filename));
       for (int i=0; i<lines.length; i++) out.println(lines[i]);
       out.close();
  }
  
  public static void echo(InputStream inp, final boolean toProgress) throws Exception
  { final LineNumberReader i = new LineNumberReader(new InputStreamReader(inp));
    new Thread()
    { public void run()
      { String line = null;
        try
        {
           while ((line = i.readLine()) != null)
           {
              if (toProgress) 
                 showProgress(line);
              else
                 System.err.println(line);
           }
           i.close();
        }
        catch (Exception exn) 
        {
           System.err.println("[Installer exception "+exn+" from subprocess]");
        }
      }
    }.start();
  }
  
  public static boolean execute(String line, boolean toProgress)
  { return execute(new String[] { line }, toProgress);
  }
  
  public static boolean execute(String[] lines, boolean toProgress)
  {
       Runtime sys = Runtime.getRuntime();
       int     status = 0;
       for (int i=0; status == 0 && i<lines.length; i++)
       { System.err.println(lines[i]); 
         System.err.flush();
         if (!lines[i].trim().startsWith("#"))
         try 
         {
            Process p = sys.exec(lines[i]);
            echo(p.getInputStream(), toProgress);
            echo(p.getErrorStream(), toProgress);
            status = p.waitFor();
         }
         catch (Exception exn)
         {
           System.err.println("[Installer exception "+exn+"]");
           showProgress("[Installer exception "+exn+"]");
         }
       }
       return status==0;
  }

  /////////////////////////// INSTALL TIME ////////////////////////////
  
  /** Use the place we were loaded from */
  public static Image getImage(Object host, String localname)
  {  Toolkit tk = Toolkit.getDefaultToolkit();
     System.err.println(localname);
     return tk.getImage(host.getClass().getResource(localname));
  }

  public static Properties getProperties(Object host) 
  {  Toolkit tk = Toolkit.getDefaultToolkit();
     Properties p = new Properties();
     try
     {
      p.load(host.getClass().getResourceAsStream(propertiesResource));
     }
     catch (Exception exn)
     {
       System.err.println(exn);
     }
     return p;
  }

  String     jarfilename = null;
  Properties prop        = null; 
  
  static TextArea progress = null;
  static int length = 0;

  public static void showProgress(String l)
  {
    progress.insert(l+"\n", length);
    length+=l.length()+1;
  }

  public static Container column(Component[] cs)
  { JToolBar r = new JToolBar();
    for (int i=0; i<cs.length; i++) 
    { if (i>0) r.addSeparator(new Dimension(10, 0));
      r.add(cs[i]);
    }
    return r;
  }

  JTextField installDirField = new JTextField();


  public install(String filename)
  {  
     prop                  = getProperties(this);
     jarfilename           = filename==null?prop.getProperty("-jar"):filename;
     
     String     splashFileName = prop.getProperty("-splash");
     String     installDirName = prop.getProperty("-installdir", ".");
     String     appName        = prop.getProperty("-app", "Jape");
     String     tfont          = prop.getProperty("-tfont", "Monospaced-PLAIN-14");
     String     bfont          = prop.getProperty("-bfont", "SansSerif-PLAIN-14");
     String     caption        = "Installer "+appName+" from "+jarfilename;
     Font       tFont          = Font.decode(tfont); //new Font("SansSerif", Font.BOLD, 18);
     Font       bFont          = Font.decode(bfont); //new Font("SansSerif", Font.BOLD, 18);
     JFrame     frame          = new JFrame(caption);
     Container  content        = frame.getContentPane();
     JButton    install        = new JButton("Install "+appName);
     JButton    exit           = new JButton("Exit Now");
     JLabel     label          = new JLabel(prop.getProperty("-label", ""));
     progress = new TextArea("", 15, 72);
     progress.setFont(tFont);
     install.addActionListener(this);
     exit.addActionListener(this);
     install.setFont(bFont);
     exit.setFont(bFont);
     label.setFont(bFont);
     installDirField.setText(installDirName);    

     // prop.list(System.err);
     
     
     if (splashFileName!=null)
     {
       ImageIcon i = new ImageIcon(getImage(this, splashFileName));
       JLabel icon = new JLabel(i, SwingConstants.CENTER);
       content.add(icon, BorderLayout.NORTH);
     }
     Component[] buttons = new Component[]
     {  label, /* installDirField, */ exit, install
     };
     content.add(progress,        BorderLayout.CENTER);
     content.add(column(buttons), BorderLayout.SOUTH);
     frame.setLocation(new Point(200, 200));
     frame.pack();
     frame.setVisible(true);
     showProgress("Installing for "+System.getProperty("os.name", "unknown OS type (assumed to be Unixoid)"));
  }

  public void actionPerformed(ActionEvent e)
  { String c = e.getActionCommand();
    if (c.startsWith("Install"))
    { 
       try
       { 
         boolean isWindows = System.getProperty("os.name", "Unix").startsWith("Windows");
         String  postClass = prop.getProperty(isWindows?"-classwindows":"-classunix");
         unJar(installDirField.getText().trim(), new FileInputStream(jarfilename));
         String shell = prop.getProperty(isWindows?"-cmdwindows":"-cmdunix");
         String shell1 = prop.getProperty(isWindows?"-cmdwindows1":"-cmdunix1");

         if (postClass!=null)
         { Class  theClass = Class.forName(postClass);
           showProgress("[Running post-install class "+postClass+"]");
           Object theObject = theClass.newInstance(); 
           showProgress("[Ran "+postClass+" successfully]");
         }
         
         if (shell!=null)
         { 
            execute(shell, true);
         }
         
         if (shell1!=null)
         { 
            execute(shell1, true);
         }
         
       }
       catch (Exception exn)
       {
          System.err.println(exn);
          showProgress("Exception during post-install phase "+exn);
       }
    }
    else
    if (c.startsWith("Exit"))
    {
       System.exit(0);
    }
  }


  /////////////////////////// JAR UNPACKING ////////////////////////////


  /**
        Ensure that the directories in the given path exist.
  */
  public static void mkDir(String path)
  { File dir = new File(path);
    if (dir.mkdirs()) System.err.println("[made "+path+"]");
  }
  
  /**
        Ensure that the directories in the given path exist.
  */
  public static void mkParentDir(String path)
  { File dir = new File(path);
    mkDir(dir.getParent());
  }
  
  /**
        Unpack the jar file on the input stream into
        the directory named in dir.
  */
  public static void unJar(String rootPath, FileInputStream in)
  throws IOException, ZipException
  { mkDir(rootPath);
    ZipInputStream zip    = new ZipInputStream(in);
    ZipEntry       entry  = null;
    byte[]         buffer = new byte[50*1024];
    while ((entry = zip.getNextEntry())!=null)
    { String path = rootPath + File.separatorChar + entry.getName();
      System.err.println(path);
      showProgress(path);
      if (entry.isDirectory())
      { 
        mkDir(path);
      }
      else
      { mkParentDir(path);
        FileOutputStream out = new FileOutputStream(path);
        int len;
        while ((len = zip.read(buffer, 0, buffer.length)) >=0 )
        { 
          out.write(buffer, 0, len);
        }
        out.close();
        zip.closeEntry();
      }
    }
    zip.close();    
  }

  
}













