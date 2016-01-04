package net.laraifox.particlesandbox.debug;

import net.laraifox.particlesandbox.core.InputHandler;
import net.laraifox.particlesandbox.core.ProgramDisplay;

public class Debugger {
	private static ProgramDisplay display;

	private static EnumDebugOverlay debugOverlay;

	private Debugger() {

	}

	public static void initialize(ProgramDisplay display) {
		Debugger.display = display;

		Debugger.debugOverlay = EnumDebugOverlay.None;
	}

	public static void update() {
		if (InputHandler.isKeyPressed(InputHandler.KEY_F1)) {
			if (debugOverlay == EnumDebugOverlay.Help) {
				debugOverlay = EnumDebugOverlay.None;
			} else {
				debugOverlay = EnumDebugOverlay.Help;
			}
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F2)) {
			if (debugOverlay == EnumDebugOverlay.DebugLogHUD) {
				debugOverlay = EnumDebugOverlay.None;
			} else {
				debugOverlay = EnumDebugOverlay.DebugLogHUD;
			}
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F3)) {
			if (debugOverlay == EnumDebugOverlay.Performance) {
				debugOverlay = EnumDebugOverlay.None;
			} else {
				debugOverlay = EnumDebugOverlay.Performance;
			}
		}
	}

	public static void render() {
		switch (debugOverlay) {
		case Help:

			break;
		case DebugLogHUD:

			break;
		case Performance:

			break;
		default:
			break;
		}
	}
}
