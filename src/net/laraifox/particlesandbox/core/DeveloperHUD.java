package net.laraifox.particlesandbox.core;

public class DeveloperHUD {
	private enum EnumDeveloperHUDState {
		None, Help, Settings, Performance;

		public static EnumDeveloperHUDState checkState(EnumDeveloperHUDState currentState, EnumDeveloperHUDState otherState) {
			if (currentState == otherState) {
				return EnumDeveloperHUDState.None;
			}

			return otherState;
		}
	}

	private EnumDeveloperHUDState currentState;

	public DeveloperHUD() {
		this.currentState = EnumDeveloperHUDState.None;
	}

	public void update() {
		if (InputHandler.isKeyPressed(InputHandler.KEY_F1)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.Help);
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F2)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.Settings);
		} else if (InputHandler.isKeyPressed(InputHandler.KEY_F3)) {
			currentState = EnumDeveloperHUDState.checkState(currentState, EnumDeveloperHUDState.Performance);
		}
	}

	public void render() {
		switch (currentState) {
		case Help:

			break;
		case Settings:

			break;
		case Performance:

			break;
		default:
			break;
		}
	}
}
