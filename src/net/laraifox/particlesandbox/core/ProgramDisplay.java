package net.laraifox.particlesandbox.core;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import net.laraifox.particlesandbox.debug.Debugger;

public class ProgramDisplay {
	private String title;
	private float width, height;
	private boolean isFullscreen;
	private boolean isResizable;
	private boolean isVSyncEnabled;

	private PixelFormat pixelFormat;
	private ContextAttribs contextAttribs;

	private boolean isInitialized;
	private boolean isRunning;

	private int framerate, updaterate, tickrate;
	private int updates, ups;
	private int frames, fps;

	private GameManager gameManager;

	public ProgramDisplay(String title) {
		this.title = title;
		this.width = Configuration.getFloat(EnumConfigKey.DISPLAY_WIDTH);
		this.height = Configuration.getFloat(EnumConfigKey.DISPLAY_HEIGHT);
		this.isFullscreen = Configuration.getBoolean(EnumConfigKey.DISPLAY_FULLSCREEN);
		this.isResizable = Configuration.getBoolean(EnumConfigKey.DISPLAY_RESIZABLE);
		this.isVSyncEnabled = Configuration.getBoolean(EnumConfigKey.DISPLAY_VSYNC);

<<<<<<< HEAD
		this.pixelFormat = new PixelFormat().withSamples(8);
=======
		this.pixelFormat = new PixelFormat(8, 8, 1, 1, 8, 0, 8, 8, false);
>>>>>>> 1e4336fbcc99d1113f1815939ad9cf91fcfffec3
		this.contextAttribs = new ContextAttribs();

		this.isInitialized = false;
		this.isRunning = false;

		this.framerate = 60;
		this.updaterate = 60;
		this.tickrate = 1;
		this.updates = 0;
		this.frames = 0;
		this.ups = 0;
		this.fps = 0;

		try {
			this.initialize();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public final void initialize() throws LWJGLException {
		if (isInitialized)
			return;

		isInitialized = true;
		initializeDisplay();
		initializeOpenGL();
		initializeVariables();
	}

	private void initializeDisplay() throws LWJGLException {
		Display.setTitle(title);
		// Display.setDisplayMode((new DisplayMode((int) width, (int) height)));
		// Display.setFullscreen(true);
		this.setDisplayMode((int) width, (int) height, isFullscreen);
		Display.setResizable(isResizable);
		Display.setVSyncEnabled(true);
		Display.create(pixelFormat, contextAttribs);
	}

	private void setDisplayMode(int width, int height, boolean fullscreen) {
		// return if requested DisplayMode is already set
		if ((Display.getDisplayMode().getWidth() == width) && (Display.getDisplayMode().getHeight() == height) && (Display.isFullscreen() == fullscreen)) {
			return;
		}

		try {
			DisplayMode targetDisplayMode = null;

			if (fullscreen) {
				DisplayMode[] modes = Display.getAvailableDisplayModes();
				int freq = 0;

				for (int i = 0; i < modes.length; i++) {
					DisplayMode current = modes[i];

					if ((current.getWidth() == width) && (current.getHeight() == height)) {
						if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
							if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
								targetDisplayMode = current;
								freq = targetDisplayMode.getFrequency();
							}
						}

						// if we've found a match for bpp and frequence against the
						// original display mode then it's probably best to go for this one
						// since it's most likely compatible with the monitor
						if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel())
							&& (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
							targetDisplayMode = current;
							break;
						}
					}
				}
			} else {
				targetDisplayMode = new DisplayMode(width, height);
			}

			if (targetDisplayMode == null) {
				System.out.println("Failed to find value mode: " + width + "x" + height + " fs=" + fullscreen);
				return;
			}

			Display.setDisplayMode(targetDisplayMode);
			Display.setFullscreen(fullscreen);
		} catch (LWJGLException e) {
			System.out.println("Unable to setup mode " + width + "x" + height + " fullscreen=" + fullscreen + e);
		}
	}

	private void initializeOpenGL() {
		initializeDefaultGLProjection();
		initializeOpenGLDefaults();
	}

	private final void initializeDefaultGLProjection() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, width, 0, height, -1.0f, 1.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}

	private final void initializeOpenGLDefaults() {
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		GL11.glDisable(GL11.GL_DEPTH_TEST);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	private void initializeVariables() {
		Debugger.initialize(this);

		this.gameManager = new GameManager(width, height);
	}

	public final void start() {
		if (isRunning)
			return;

		if (isInitialized) {
			isRunning = true;
			this.gameLoop();
		}
	}

	public final void stop() {
		if (!isRunning)
			return;

		isRunning = false;
	}

	private final void gameLoop() {
		long previousDeltaTime = System.nanoTime();
		long previousTick = System.nanoTime();
		long previousUpdate = System.nanoTime();
		long nanosecondsPerTick = (long) ((float) 1000000000 / (float) tickrate);
		long nanosecondsPerUpdate = (long) ((float) 1000000000 / (float) updaterate);

		previousUpdate -= nanosecondsPerUpdate;

		while (!Display.isCloseRequested() && isRunning) {
			long currentTime = System.nanoTime();

			if (currentTime - previousTick >= nanosecondsPerTick) {
				previousTick += nanosecondsPerTick;
				ups = (int) (updates / (1.0f / tickrate));
				updates = 0;
				fps = (int) (frames / (1.0f / tickrate));
				frames = 0;
				this.tick();
			}

			if (currentTime - previousUpdate >= nanosecondsPerUpdate) {
				previousUpdate += nanosecondsPerUpdate;
				float delta = (float) (currentTime - previousDeltaTime) / (float) nanosecondsPerUpdate;
				previousDeltaTime = currentTime;
				this.update(delta);
				updates++;
			}

			this.render();
			frames++;

			Display.update();
			Display.sync(framerate);
		}

		isInitialized = false;
		this.cleanUp();
		Display.destroy();
	}

	private void cleanUp() {

	}

	private void tick() {
		Display.setTitle(title + " - FPS: " + getCurrentFPS());
	}

	private void update(float delta) {
		gameManager.update(delta);

		Debugger.update();
	}

	private void render() {
		gameManager.render();

		Debugger.render();
	}

	public final String getTitle() {
		return title;
	}

	public final void setTitle(String title) {
		this.title = title;
	}

	public final float getWidth() {
		return width;
	}

	public final void setWidth(float width) {
		this.width = width;
	}

	public final float getHeight() {
		return height;
	}

	public final void setHeight(float height) {
		this.height = height;
	}

	public final boolean isFullscreen() {
		return isFullscreen;
	}

	public final void setFullscreen(boolean isFullscreen) {
		this.isFullscreen = isFullscreen;
	}

	public final boolean isResizable() {
		return isResizable;
	}

	public final void setResizable(boolean isResizable) {
		this.isResizable = isResizable;
	}

	public final boolean isVSyncEnabled() {
		return isVSyncEnabled;
	}

	public final void setVSyncEnabled(boolean isVSyncEnabled) {
		this.isVSyncEnabled = isVSyncEnabled;
	}

	public final PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	public final void setPixelFormat(PixelFormat pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public final ContextAttribs getContextAttribs() {
		return contextAttribs;
	}

	public final void setContextAttribs(ContextAttribs contextAttribs) {
		this.contextAttribs = contextAttribs;
	}

	public final boolean isInitialized() {
		return isInitialized;
	}

	public final void setInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}

	public final boolean isRunning() {
		return isRunning;
	}

	public final void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public final int getFramerate() {
		return framerate;
	}

	public final void setFramerate(int framerate) {
		this.framerate = framerate;
	}

	public final int getUpdaterate() {
		return updaterate;
	}

	public final void setUpdaterate(int updaterate) {
		this.updaterate = updaterate;
	}

	public final int getCurrentUPS() {
		return ups;
	}

	public final int getCurrentFPS() {
		return fps;
	}
}
