package net.laraifox.particlesandbox.core;

public class DeveloperHUD {
	private enum EnumDeveloperHUDState {
		NONE, HELP, SETTINGS, PERFORMANCE;

		public static EnumDeveloperHUDState checkState(EnumDeveloperHUDState currentState, EnumDeveloperHUDState otherState) {
			if (currentState == otherState) {
				return EnumDeveloperHUDState.NONE;
			}

			return otherState;
		}
	}

	private EnumDeveloperHUDState currentState;

	public DeveloperHUD() {
		this.currentState = EnumDeveloperHUDState.NONE;
	}

	public void update() {
		if (InputHandler.isKeyPressed(InputHandler.KEY_F1)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.HELP);
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F2)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.SETTINGS);
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F3)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.PERFORMANCE);
		}
	}

	public void render() {
		switch (currentState) {
		case HELP:

			break;
		case SETTINGS:

			break;
		case PERFORMANCE:

			break;
		default:
			break;
		}
	}
}
