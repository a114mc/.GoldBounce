package net.ccbluex.liquidbounce.utils.modernsplash;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;
import static net.minecraftforge.fml.common.asm.FMLSanityChecker.fmlLocation;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.SharedDrawable;
import org.lwjgl.util.glu.GLU;


/**
 * @deprecated not a stable API, will break, don't use this yet
 */

public class CustomSplash
{
    private static Drawable d;
    private static volatile boolean pause = false;
    private static volatile boolean done = false;
    private static Thread thread;
    private static volatile Throwable threadError;
    private static int angle = 0;
    private static final Lock lock = new ReentrantLock(true);
    private static SplashFontRenderer fontRenderer;

    private static final IResourcePack mcPack = Minecraft.getMinecraft().mcDefaultResourcePack;
    private static final IResourcePack fmlPack = createResourcePack(fmlLocation);
    private static IResourcePack miscPack;

    private static Texture fontTexture;
    private static Texture logoTexture;
    private static Texture forgeTexture;

    private static Properties config;

    private static boolean enabled;
    private static boolean forgeLogo;
    private static boolean rotate;
    private static int logoOffset;
    private static int backgroundColor;
    private static int fontColor;
    private static int logoColor;
    private static int barBorderColor;
    private static int barColor;
    private static int barBackgroundColor;
    private static boolean showMemory;
    private static boolean showTotalMemoryLine;

    public static boolean displayStartupTimeOnMainMenu = true;
    public static boolean enableTimer = true;
    private static int memoryGoodColor;
    private static int memoryWarnColor;
    private static int memoryLowColor;
    private static float memoryColorPercent;
    private static long memoryColorChangeTime;
    public static final Semaphore mutex = new Semaphore(1);

    private static String getString(String name, String def)
    {
        String value = config.getProperty(name, def);
        config.setProperty(name, value);
        return value;
    }

    private static boolean getBool(String name, boolean def)
    {
        return Boolean.parseBoolean(getString(name, Boolean.toString(def)));
    }

    private static int getInt(String name, int def)
    {
        return Integer.decode(getString(name, Integer.toString(def)));
    }

    private static int getHex(String name, int def)
    {
        return Integer.decode(getString(name, "0x" + Integer.toString(def, 16).toUpperCase()));
    }

    public static void start()
    {
        File configFile = new File(Minecraft.getMinecraft().mcDataDir, "config/splash.properties");
        FileReader r = null;
        config = new Properties();
        try
        {
            r = new FileReader(configFile);
            config.load(r);
        }
        catch(IOException e)
        {
            FMLLog.info("Could not load splash.properties, will create a default one");
        }
        finally
        {
            IOUtils.closeQuietly(r);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmm");
        int now = Integer.parseInt(formatter.format(LocalDateTime.now()));

        // Enable if we have the flag, and there's either no optifine, or optifine has added a key to the blackboard ("optifine.ForgeSplashCompatible")
        // Optifine authors - add this key to the blackboard if you feel your modifications are now compatible with this code.
        enabled =             getBool("enabled",              true) && ( (! FMLClientHandler.instance().hasOptifine()) || Launch.blackboard.containsKey("optifine.ForgeSplashCompatible"));
        rotate =              getBool("rotate",              false);
        forgeLogo =           getBool("forgeLogo",           false);
        showMemory =          getBool("showMemory",           true);
        showTotalMemoryLine = getBool("showTotalMemoryLine", false);
        enableTimer =         getBool("enableTimer",          true);

        logoOffset =         getInt("logoOffset",    0);

        fontColor =          getHex("font",          0xFFFFFF);
        logoColor =          getHex("logo",          0xFFFFFF);
        backgroundColor =    getHex("background",    0xEF323D);
        barBorderColor =     getHex("barBorder",     0xFFFFFF);
        barColor =           getHex("bar",           0xFFFFFF);
        barBackgroundColor = getHex("barBackground", 0xEF323D);
        memoryGoodColor =    getHex("memoryGood",    0xFFFFFF);
        memoryWarnColor =    getHex("memoryWarn",    0xFFFFFF);
        memoryLowColor =     getHex("memoryLow",     0xFFFFFF);

        displayStartupTimeOnMainMenu = getBool("timeOnMainMenu", true);

        boolean darkModeOnly = getBool("darkModeOnly", false);

        int darkStartTime = getInt("darkStartTime", 2300);
        int darkEndTime =   getInt("darkEndTime", 600);

        int backgroundColorNight =    getHex("backgroundDark", 0x202020);
        int fontColorNight =          getHex("fontDark", 0x606060);
        int logoColorNight =          getHex("logoDark", 0x999999);
        int barBorderColorNight =     getHex("barBorderDark", 0x4E4E4E);
        int barColorNight =           getHex("barDark", 0x4E4E4E);
        int barBackgroundColorNight = getHex("barBackgroundDark", 0x202020);
        int memoryGoodColorNight =    getHex("memoryGoodDark", 0x4E4E4E);
        int memoryWarnColorNight =    getHex("memoryWarnDark", 0x4E4E4E);
        int memoryLowColorNight =     getHex("memoryLowDark", 0x4E4E4E);

        if(darkModeOnly || (darkEndTime >= darkStartTime ? (now >= darkStartTime && now < darkEndTime) : (now >= darkStartTime || now <= darkEndTime))) {
            backgroundColor    = backgroundColorNight;
            fontColor          = fontColorNight;
            logoColor          = logoColorNight;
            barBorderColor     = barBorderColorNight;
            barColor           = barColorNight;
            barBackgroundColor = barBackgroundColorNight;
            memoryGoodColor    = memoryGoodColorNight;
            memoryWarnColor    = memoryWarnColorNight;
            memoryLowColor     = memoryLowColorNight;
        }
        final ResourceLocation fontLoc = new ResourceLocation(getString("fontTexture", "textures/font/ascii.png"));
        final ResourceLocation logoLoc = new ResourceLocation("liquidbounce/icons/mojang.png");
        final ResourceLocation forgeLoc = new ResourceLocation(getString("forgeTexture", "fml:textures/gui/forge.gif"));

        File miscPackFile = new File(Minecraft.getMinecraft().mcDataDir, getString("resourcePackPath", "resources"));

        FileWriter w = null;
        try
        {
            w = new FileWriter(configFile);
            config.store(w, "Splash screen properties");
        }
        catch(IOException e)
        {
            FMLLog.log(Level.ERROR, e, "Could not save the splash.properties file");
        }
        finally
        {
            IOUtils.closeQuietly(w);
        }

        miscPack = createResourcePack(miscPackFile);

        if(!enabled) return;
        // getting debug info out of the way, while we still can
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable()
        {
            public String call() throws Exception
            {
                return "' Vendor: '" + glGetString(GL_VENDOR) +
                        "' Version: '" + glGetString(GL_VERSION) +
                        "' Renderer: '" + glGetString(GL_RENDERER) +
                        "'";
            }

            public String getLabel()
            {
                return "GL info";
            }
        });
        CrashReport report = CrashReport.makeCrashReport(new Throwable()
        {
            @Override public String getMessage(){ return "This is just a prompt for computer specs to be printed. THIS IS NOT A ERROR"; }
            @Override public void printStackTrace(final PrintWriter s){ s.println(getMessage()); }
            @Override public void printStackTrace(final PrintStream s) { s.println(getMessage()); }
        }, "Loading screen debug info");
        System.out.println(report.getCompleteReport());

        try
        {
            d = new SharedDrawable(Display.getDrawable());
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Thread mainThread = Thread.currentThread();
        thread = new Thread(new Runnable()
        {
            private final int barWidth = 400;
            private final int barHeight = 20;
            private final int textHeight2 = 20;
            private final int barOffset = 45;

            public void run()
            {
                setGL();
                fontTexture = new Texture(fontLoc);
                logoTexture = new Texture(logoLoc);
                forgeTexture = new Texture(forgeLoc);
                glEnable(GL_TEXTURE_2D);
                fontRenderer = new SplashFontRenderer();
                glDisable(GL_TEXTURE_2D);
                while(!done)
                {
                    ProgressManager.ProgressBar first = null;
                    ProgressManager.ProgressBar penult = null;
                    ProgressManager.ProgressBar last = null;
                    Iterator<ProgressManager.ProgressBar> i = ProgressManager.barIterator();
                    while(i.hasNext())
                    {
                        if(first == null) first = i.next();
                        else
                        {
                            penult = last;
                            last = i.next();
                        }
                    }

                    glClear(GL_COLOR_BUFFER_BIT);

                    // matrix setup
                    int w = Display.getWidth();
                    int h = Display.getHeight();
                    glViewport(0, 0, w, h);
                    glMatrixMode(GL_PROJECTION);
                    glLoadIdentity();
                    glOrtho(320 - w/2, 320 + w/2, 240 + h/2, 240 - h/2, -1, 1);
                    glMatrixMode(GL_MODELVIEW);
                    glLoadIdentity();

                    // mojang logo
                    setColor(logoColor);
                    glEnable(GL_TEXTURE_2D);
                    logoTexture.bind();
                    glBegin(GL_QUADS);
                    logoTexture.texCoord(0, 0, 0);
                    glVertex2f(320 - 256, 240 - 256);
                    logoTexture.texCoord(0, 0, 1);
                    glVertex2f(320 - 256, 240 + 256);
                    logoTexture.texCoord(0, 1, 1);
                    glVertex2f(320 + 256, 240 + 256);
                    logoTexture.texCoord(0, 1, 0);
                    glVertex2f(320 + 256, 240 - 256);
                    glEnd();
                    glDisable(GL_TEXTURE_2D);

                    if (showMemory)
                    {
                        glPushMatrix();
                        glTranslatef(320 - (float) barWidth / 2, 20, 0);
                        drawMemoryBar();
                        glPopMatrix();
                    }

                    // timer
                    if(enableTimer) {
                        glPushMatrix();
                        setColor(fontColor);
                        glTranslatef(320 - Display.getWidth() / 2 + 4, 240 + Display.getHeight() / 2 - textHeight2, 0);
                        glScalef(2, 2, 1);
                        glEnable(GL_TEXTURE_2D);
                        String renderString = getString();
                        fontRenderer.drawString(renderString, 0, 0, fontColor);
                        glDisable(GL_TEXTURE_2D);
                        glPopMatrix();
                    }

                    // bars
                    if(first != null)
                    {
                        glPushMatrix();
                        glTranslatef(320 - (float)barWidth / 2, 310, 0);
                        drawBar(first);
                        if(penult != null)
                        {
                            glTranslatef(0, barOffset, 0);
                            drawBar(penult);
                        }
                        if(last != null)
                        {
                            glTranslatef(0, barOffset, 0);
                            drawBar(last);
                        }
                        glPopMatrix();
                    }

                    angle += 1;

                    // forge logo
                    if(forgeLogo) {
                        setColor(backgroundColor);
                        float fw = (float) forgeTexture.getWidth() / 2 / 2;
                        float fh = (float) forgeTexture.getHeight() / 2 / 2;
                        if (rotate) {
                            float sh = Math.max(fw, fh);
                            glTranslatef(320 + w / 2 - sh - logoOffset, 240 + h / 2 - sh - logoOffset, 0);
                            glRotatef(angle, 0, 0, 1);
                        } else {
                            glTranslatef(320 + w / 2 - fw - logoOffset, 240 + h / 2 - fh - logoOffset, 0);
                        }
                        int f = (angle / 10) % forgeTexture.getFrames();
                        glEnable(GL_TEXTURE_2D);
                        forgeTexture.bind();
                        glBegin(GL_QUADS);
                        forgeTexture.texCoord(f, 0, 0);
                        glVertex2f(-fw, -fh);
                        forgeTexture.texCoord(f, 0, 1);
                        glVertex2f(-fw, fh);
                        forgeTexture.texCoord(f, 1, 1);
                        glVertex2f(fw, fh);
                        forgeTexture.texCoord(f, 1, 0);
                        glVertex2f(fw, -fh);
                        glEnd();
                        glDisable(GL_TEXTURE_2D);
                    }
                    // We use mutex to indicate safely to the main thread that we're taking the display global lock
                    // So the main thread can skip processing messages while we're updating.
                    // There are system setups where this call can pause for a while, because the GL implementation
                    // is trying to impose a framerate or other thing is occurring. Without the mutex, the main
                    // thread would delay waiting for the same global display lock
                    mutex.acquireUninterruptibly();
                    Display.update();
                    // As soon as we're done, we release the mutex. The other thread can now ping the processmessages
                    // call as often as it wants until we get get back here again
                    mutex.release();
                    if(pause)
                    {
                        clearGL();
                        setGL();
                    }
                    Display.sync(100);
                }
                clearGL();
            }

            private String getString(){
                long startupTime = ManagementFactory.getRuntimeMXBean().getUptime();

                if(ModernSplash.doneTime > 0) startupTime = ModernSplash.doneTime;

                long minutes = (startupTime / 1000) / 60;
                long seconds = (startupTime / 1000) % 60;

                String str = "Startup: " + minutes + "m " + seconds + "s";

                if(MSLoadingPlugin.expectedTime > 0){
                    long ex_minutes = (MSLoadingPlugin.expectedTime / 1000) / 60;
                    long ex_seconds = (MSLoadingPlugin.expectedTime / 1000) % 60;

                    str += " / ~" + ex_minutes + "m " + ex_seconds + "s";
                }

                return str;
            }

            private void setColor(int color)
            {
                glColor3ub((byte)((color >> 16) & 0xFF), (byte)((color >> 8) & 0xFF), (byte)(color & 0xFF));
            }

            private void drawBox(int w, int h)
            {
                glBegin(GL_QUADS);
                glVertex2f(0, 0);
                glVertex2f(0, h);
                glVertex2f(w, h);
                glVertex2f(w, 0);
                glEnd();
            }

            private void drawBar(ProgressManager.ProgressBar b)
            {
                String progress = "" + b.getStep() + "/" + b.getSteps();
                glPushMatrix();
                // title - message
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(b.getTitle() + " " + progress + " - " + b.getMessage(), 0, 0, fontColor);
                glDisable(GL_TEXTURE_2D);
                glPopMatrix();
                // border
                glPushMatrix();
                glTranslatef(0, textHeight2, 0);
                setColor(barBorderColor);
                drawBox(barWidth, barHeight);
                // interior
                setColor(barBackgroundColor);
                glTranslatef(2, 2, 0);
                drawBox(barWidth - 4, barHeight - 4);
                // slidy part
                setColor(barColor);
                glTranslatef(2, 2, 0);
                drawBox((barWidth - 8) * (b.getStep() + 1) / (b.getSteps() + 1), barHeight - 8); // Step can sometimes be 0.
                // progress text
                /*
                glTranslatef(((float)barWidth - 2) / 2 - fontRenderer.getStringWidth(progress), 2, 0);
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(progress, 0, 0, 0x000000);*/
                glPopMatrix();
            }
            private void drawMemoryBar() {
                int maxMemory = bytesToMb(Runtime.getRuntime().maxMemory());
                int totalMemory = bytesToMb(Runtime.getRuntime().totalMemory());
                int freeMemory = bytesToMb(Runtime.getRuntime().freeMemory());
                int usedMemory = totalMemory - freeMemory;
                float usedMemoryPercent = usedMemory / (float) maxMemory;
                String progress = getMemoryString(usedMemory) + " / " + getMemoryString(maxMemory);
                glPushMatrix();
                // title - message
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString("Memory Usage : " + progress, 0, 0, fontColor);
                glDisable(GL_TEXTURE_2D);
                glPopMatrix();
                // border
                glPushMatrix();
                glTranslatef(0, textHeight2, 0);
                setColor(barBorderColor);
                drawBox(barWidth, barHeight);
                // interior
                setColor(barBackgroundColor);
                glTranslatef(2, 2, 0);
                drawBox(barWidth - 4, barHeight - 4);
                // slidy part

                long time = System.currentTimeMillis();
                if (usedMemoryPercent > memoryColorPercent || (time - memoryColorChangeTime > 1000))
                {
                    memoryColorChangeTime = time;
                    memoryColorPercent = usedMemoryPercent;
                }

                int memoryBarColor;
                if (memoryColorPercent < 0.75f)
                {
                    memoryBarColor = memoryGoodColor;
                }
                else if (memoryColorPercent < 0.85f)
                {
                    memoryBarColor = memoryWarnColor;
                }
                else
                {
                    memoryBarColor = memoryLowColor;
                }
                if(showTotalMemoryLine) {
                    setColor(memoryLowColor);
                    glPushMatrix();
                    glTranslatef((barWidth - 8) * (totalMemory) / (maxMemory) - 2, 2, 0);
                    drawBox(2, barHeight - 8);
                    glPopMatrix();
                }
                setColor(memoryBarColor);
                glTranslatef(2, 2, 0);
                drawBox((barWidth - 8) * (usedMemory) / (maxMemory), barHeight - 8);

                // progress text
                //String progress = getMemoryString(usedMemory) + " / " + getMemoryString(maxMemory);
                /*glTranslatef(((float)barWidth - 2) / 2 - fontRenderer.getStringWidth(progress), 2, 0);
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(progress, 0, 0, 0x000000);*/
                glPopMatrix();
            }

            private String getMemoryString(int memory)
            {
                return StringUtils.leftPad(Integer.toString(memory), 4, ' ') + " MB";
            }
            private void setGL()
            {
                lock.lock();
                try
                {
                    Display.getDrawable().makeCurrent();
                }
                catch (LWJGLException e)
                {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                glClearColor((float)((backgroundColor >> 16) & 0xFF) / 0xFF, (float)((backgroundColor >> 8) & 0xFF) / 0xFF, (float)(backgroundColor & 0xFF) / 0xFF, 1);
                glDisable(GL_LIGHTING);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }

            private void clearGL()
            {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayWidth = Display.getWidth();
                mc.displayHeight = Display.getHeight();
                mc.resize(mc.displayWidth, mc.displayHeight);
                glClearColor(1, 1, 1, 1);
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
                glEnable(GL_ALPHA_TEST);
                glAlphaFunc(GL_GREATER, .1f);
                try
                {
                    Display.getDrawable().releaseContext();
                }
                catch (LWJGLException e)
                {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                finally
                {
                    lock.unlock();
                }
            }
        });
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
        {
            public void uncaughtException(Thread t, Throwable e)
            {
                FMLLog.log(Level.ERROR, e, "Splash thread Exception");
                threadError = e;
            }
        });
        thread.start();
        checkThreadState();
    }

    private static void checkThreadState()
    {
        if(thread.getState() == Thread.State.TERMINATED || threadError != null)
        {
            throw new IllegalStateException("Splash thread", threadError);
        }
    }
    /**
     * Call before you need to explicitly modify GL context state during loading.
     * Resource loading doesn't usually require this call.
     * Call {@link #resume()} when you're done.
     * @deprecated not a stable API, will break, don't use this yet
     */
    @Deprecated
    public static void pause()
    {
        if(!enabled) return;
        checkThreadState();
        pause = true;
        lock.lock();
        try
        {
            d.releaseContext();
            Display.getDrawable().makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated not a stable API, will break, don't use this yet
     */
    @Deprecated
    public static void resume()
    {
        if(!enabled) return;
        checkThreadState();
        pause = false;
        try
        {
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        lock.unlock();
    }

    public static void finish()
    {
        if(!enabled) return;
        try
        {
            checkThreadState();
            done = true;
            thread.join();
            d.releaseContext();
            Display.getDrawable().makeCurrent();
            fontTexture.delete();
            logoTexture.delete();
            forgeTexture.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (disableSplash())
            {
                throw new EnhancedRuntimeException(e)
                {
                    @Override
                    protected void printStackTrace(EnhancedRuntimeException.WrappedPrintStream stream)
                    {
                        stream.println("SplashProgress has detected a error loading Minecraft.");
                        stream.println("This can sometimes be caused by bad video drivers.");
                        stream.println("We have automatically disabeled the new Splash Screen in config/splash.properties.");
                        stream.println("Try reloading minecraft before reporting any errors.");
                    }
                };
            }
            else
            {
                throw new EnhancedRuntimeException(e)
                {
                    @Override
                    protected void printStackTrace(WrappedPrintStream stream)
                    {
                        stream.println("SplashProgress has detected a error loading Minecraft.");
                        stream.println("This can sometimes be caused by bad video drivers.");
                        stream.println("Please try disabeling the new Splash Screen in config/splash.properties.");
                        stream.println("After doing so, try reloading minecraft before reporting any errors.");
                    }
                };
            }
        }
    }

    private static boolean disableSplash()
    {
        File configFile = new File(Minecraft.getMinecraft().mcDataDir, "config/splash.properties");
        File parent = configFile.getParentFile();
        if (!parent.exists())
            parent.mkdirs();

        FileReader r = null;
        enabled = false;
        config.setProperty("enabled", "false");

        FileWriter w = null;
        try
        {
            w = new FileWriter(configFile);
            config.store(w, "Splash screen properties");
        }
        catch(IOException e)
        {
            FMLLog.log(Level.ERROR, e, "Could not save the splash.properties file");
            return false;
        }
        finally
        {
            IOUtils.closeQuietly(w);
        }
        return true;
    }

    private static IResourcePack createResourcePack(File file)
    {
        if(file.isDirectory())
        {
            return new FolderResourcePack(file);
        }
        else
        {
            return new FileResourcePack(file);
        }
    }

    private static final IntBuffer buf = BufferUtils.createIntBuffer(4 * 1024 * 1024);

    private static class Texture
    {
        private final ResourceLocation location;
        private final int name;
        private final int width;
        private final int height;
        private final int frames;
        private final int size;

        public Texture(ResourceLocation location)
        {
            InputStream s = null;
            try
            {
                this.location = location;
                s = open(location);
                ImageInputStream stream = ImageIO.createImageInputStream(s);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if(!readers.hasNext()) throw new IOException("No suitable reader found for image" + location);
                ImageReader reader = readers.next();
                reader.setInput(stream);
                frames = reader.getNumImages(true);
                BufferedImage[] images = new BufferedImage[frames];
                for(int i = 0; i < frames; i++)
                {
                    images[i] = reader.read(i);
                }
                reader.dispose();
                int size = 1;
                width = images[0].getWidth();
                height = images[0].getHeight();
                while((size / width) * (size / height) < frames) size *= 2;
                this.size = size;
                glEnable(GL_TEXTURE_2D);
                synchronized(CustomSplash.class)
                {
                    name = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, name);
                }
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer)null);
                checkGLError("Texture creation");
                for(int i = 0; i * (size / width) < frames; i++)
                {
                    for(int j = 0; i * (size / width) + j < frames && j < size / width; j++)
                    {
                        buf.clear();
                        BufferedImage image = images[i * (size / width) + j];
                        for(int k = 0; k < height; k++)
                        {
                            for(int l = 0; l < width; l++)
                            {
                                buf.put(image.getRGB(l, k));
                            }
                        }
                        buf.position(0).limit(width * height);
                        glTexSubImage2D(GL_TEXTURE_2D, 0, j * width, i * height, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buf);
                        checkGLError("Texture uploading");
                    }
                }
                glBindTexture(GL_TEXTURE_2D, 0);
                glDisable(GL_TEXTURE_2D);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            finally
            {
                IOUtils.closeQuietly(s);
            }
        }

        public ResourceLocation getLocation()
        {
            return location;
        }

        public int getName()
        {
            return name;
        }

        public int getWidth()
        {
            return width;
        }

        public int getHeight()
        {
            return height;
        }

        public int getFrames()
        {
            return frames;
        }

        public int getSize()
        {
            return size;
        }

        public void bind()
        {
            glBindTexture(GL_TEXTURE_2D, name);
        }

        public void delete()
        {
            glDeleteTextures(name);
        }

        public float getU(int frame, float u)
        {
            return width * (frame % (size / width) + u) / size;
        }

        public float getV(int frame, float v)
        {
            return height * (frame / (size / width) + v) / size;
        }

        public void texCoord(int frame, float u, float v)
        {
            glTexCoord2f(getU(frame, u), getV(frame, v));
        }
    }

    private static class SplashFontRenderer extends FontRenderer
    {
        public SplashFontRenderer()
        {
            super(Minecraft.getMinecraft().gameSettings, fontTexture.getLocation(), null, false);
            super.onResourceManagerReload(null);
        }

        @Override
        protected void bindTexture(ResourceLocation location)
        {
            if(location != locationFontTexture) throw new IllegalArgumentException();
            fontTexture.bind();
        }

        @Override
        protected InputStream getResourceInputStream(ResourceLocation location) throws IOException
        {
            return Minecraft.getMinecraft().mcDefaultResourcePack.getInputStream(location);
        }
    }

    public static void drawVanillaScreen(TextureManager renderEngine) throws LWJGLException
    {
        if(!enabled)
        {
            mc.drawSplashScreen(renderEngine);
        }
    }

    public static void clearVanillaResources(TextureManager renderEngine, ResourceLocation mojangLogo)
    {
        if(!enabled)
        {
            renderEngine.deleteTexture(mojangLogo);
        }
    }

    public static void checkGLError(String where)
    {
        int err = GL11.glGetError();
        if (err != 0)
        {
            throw new IllegalStateException(where + ": " + GLU.gluErrorString(err));
        }
    }

    private static InputStream open(ResourceLocation loc) throws IOException
    {
        if(miscPack.resourceExists(loc))
        {
            return miscPack.getInputStream(loc);
        }
        else if(fmlPack.resourceExists(loc))
        {
            return fmlPack.getInputStream(loc);
        }
        return mcPack.getInputStream(loc);
    }

    private static int bytesToMb(long bytes)
    {
        return (int) (bytes / 1024L / 1024L);
    }

}