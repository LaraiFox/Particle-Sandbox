package net.laraifox.particlesandbox.core;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

public class FrameBuffer {
	private int fboID;
	private int textureID;
	private int depthBufferID;

	private int width, height;

	public FrameBuffer(int width, int height) {
		this.fboID = createFrameBuffer();
		this.textureID = createTextureAttachment(width, height);
		this.depthBufferID = createDepthBufferAttachment(width, height);

		this.width = width;
		this.height = height;
		unbindCurrentFrameBuffer();
	}

	public void cleanUp() {
		GL30.glDeleteFramebuffers(fboID);
		GL11.glDeleteTextures(textureID);
		GL30.glDeleteRenderbuffers(depthBufferID);
	}

	public void unbindCurrentFrameBuffer() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
	}

	public void bindFrameBuffer() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
		GL11.glViewport(0, 0, width, height);
	}

	private int createFrameBuffer() {
		int frameBuffer = GL30.glGenFramebuffers();
		// generate name for frame buffer
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		// create the framebuffer
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		// indicate that we will always render to color attachment 0
		return frameBuffer;
	}

	private int createTextureAttachment(int width, int height) {
		int texture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, texture, 0);
		return texture;
	}

	// private int createDepthTextureAttachment(int width, int height) {
	// int texture = GL11.glGenTextures();
	// GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
	// GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
	// GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
	// GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
	// GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture, 0);
	// return texture;
	// }

	private int createDepthBufferAttachment(int width, int height) {
		int depthBuffer = GL30.glGenRenderbuffers();
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
		return depthBuffer;
	}

	public int getTextureID() {
		return textureID;
	}

}
