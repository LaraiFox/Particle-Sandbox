package net.laraifox.particlesandbox.core;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import net.laraifox.particlesandbox.utils.FileUtils;

public class Shader {
	private class GLSLStructElement {
		public String type;
		public String name;

		public GLSLStructElement(String type, String name) {
			this.type = type;
			this.name = name;
		}
	}

	private HashMap<String, Integer> uniforms;

	private int id;

	public Shader(String vertexFilepath, String fragmentFilepath, boolean bindAttributes) throws Exception {
		this.uniforms = new HashMap<String, Integer>();

		this.createShader(vertexFilepath, fragmentFilepath, bindAttributes);
	}

	@Override
	protected void finalize() {
		GL20.glDeleteProgram(id);
	}

	private int createShader(String vertexFilepath, String fragmentFilepath, boolean bindAttributes) throws Exception {
		this.id = GL20.glCreateProgram();

		int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		String vertexShaderSrc = FileUtils.readFile(new File(vertexFilepath));
		GL20.glShaderSource(vertexShader, vertexShaderSrc);
		GL20.glCompileShader(vertexShader);

		int vertexCompileStatus = GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS);
		if (vertexCompileStatus == GL11.GL_FALSE) {
			String infoLog = GL20.glGetShaderInfoLog(vertexShader, GL20.glGetShaderi(vertexShader, GL20.GL_INFO_LOG_LENGTH));
			throw new Exception("Vertex Shader ('" + vertexFilepath + "') failed to compile!\n" + infoLog);
		}

		GL20.glAttachShader(id, vertexShader);

		int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
		String fragmentShaderSrc = FileUtils.readFile(new File(fragmentFilepath));
		GL20.glShaderSource(fragmentShader, fragmentShaderSrc);
		GL20.glCompileShader(fragmentShader);

		int fragmentCompileStatus = GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS);
		if (fragmentCompileStatus == GL11.GL_FALSE) {
			String infoLog = GL20.glGetShaderInfoLog(fragmentShader, GL20.glGetShaderi(fragmentShader, GL20.GL_INFO_LOG_LENGTH));
			throw new Exception("Fragment Shader ('" + fragmentFilepath + "') failed to compile!\n" + infoLog);
		}

		GL20.glAttachShader(id, fragmentShader);

		GL20.glLinkProgram(id);

		int programLinkStatus = GL20.glGetProgrami(id, GL20.GL_LINK_STATUS);
		if (programLinkStatus == GL11.GL_FALSE) {
			String infoLog = GL20.glGetProgramInfoLog(fragmentShader, GL20.glGetProgrami(id, GL20.GL_INFO_LOG_LENGTH));
			throw new Exception("Program ('" + vertexFilepath + "' : '" + fragmentFilepath + "') failed to link!\n" + infoLog);
		}

		GL20.glValidateProgram(id);

		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);

		if (bindAttributes) {
			bindAllAttribs(vertexShaderSrc);
		}

		getUniformLocations(vertexShaderSrc);
		getUniformLocations(fragmentShaderSrc);

		return id;
	}

	private void bindAllAttribs(String shaderSrc) {
		final String ATTRIBUTE_KEYWORD = new String("attribute");

		int currentAttribIndex = 0;
		int attributeStartLocation = shaderSrc.indexOf(ATTRIBUTE_KEYWORD);
		while (attributeStartLocation != -1) {
			if (attributeStartLocation == 0 || !Character.isWhitespace(shaderSrc.charAt(attributeStartLocation - 1)) && shaderSrc.charAt(attributeStartLocation - 1) != ';'
				|| !Character.isWhitespace(shaderSrc.charAt(attributeStartLocation + ATTRIBUTE_KEYWORD.length()))) {
				continue;
			}

			int startIndex = attributeStartLocation + ATTRIBUTE_KEYWORD.length() + 1;
			int endIndex = shaderSrc.indexOf(";", startIndex);

			String attributeLine = shaderSrc.substring(startIndex, endIndex);

			String attributeName = attributeLine.substring(attributeLine.indexOf(" ") + 1, attributeLine.length()).trim();

			this.bindAttribLocation(currentAttribIndex, attributeName);
//			System.out.println("Bound attribute '" + attributeName + "' to index: " + currentAttribIndex);
			currentAttribIndex++;

			attributeStartLocation = shaderSrc.indexOf(ATTRIBUTE_KEYWORD, attributeStartLocation + ATTRIBUTE_KEYWORD.length());
		}
	}

	private HashMap<String, ArrayList<GLSLStructElement>> findUniformStructs(String shaderSrc) {
		HashMap<String, ArrayList<GLSLStructElement>> result = new HashMap<String, ArrayList<GLSLStructElement>>();

		final String STRUCT_KEYWORD = new String("struct");

		int structStartLocation = shaderSrc.indexOf(STRUCT_KEYWORD);
		while (structStartLocation != -1) {
			if (structStartLocation == 0 || !Character.isWhitespace(shaderSrc.charAt(structStartLocation - 1)) && shaderSrc.charAt(structStartLocation - 1) != ';'
				|| !Character.isWhitespace(shaderSrc.charAt(structStartLocation + STRUCT_KEYWORD.length()))) {
				continue;
			}

			int nameStartIndex = structStartLocation + STRUCT_KEYWORD.length() + 1;
			int braceStartIndex = shaderSrc.indexOf("{", nameStartIndex);
			int braceEndIndex = shaderSrc.indexOf("}", braceStartIndex);

			String structName = shaderSrc.substring(nameStartIndex, braceStartIndex).trim();

			ArrayList<GLSLStructElement> structElements = new ArrayList<GLSLStructElement>();

			int lineSemicolonIndex = shaderSrc.indexOf(";", braceStartIndex);
			while (lineSemicolonIndex != -1 && lineSemicolonIndex < braceEndIndex) {
				int elementNameStartIndex = lineSemicolonIndex;
				while (!Character.isWhitespace(shaderSrc.charAt(elementNameStartIndex - 1))) {
					elementNameStartIndex--;
				}

				int elementTypeEndIndex = elementNameStartIndex;
				int elementTypeStartIndex = elementTypeEndIndex;
				while (!Character.isWhitespace(shaderSrc.charAt(elementTypeStartIndex - 1))) {
					elementTypeStartIndex--;
				}

				String elementType = shaderSrc.substring(elementTypeStartIndex, elementTypeEndIndex).trim();
				String elementName = shaderSrc.substring(elementNameStartIndex, lineSemicolonIndex).trim();

				structElements.add(new GLSLStructElement(elementType, elementName));

				lineSemicolonIndex = shaderSrc.indexOf(";", lineSemicolonIndex + 1);
			}

			result.put(structName, structElements);

			structStartLocation = shaderSrc.indexOf(STRUCT_KEYWORD, structStartLocation + STRUCT_KEYWORD.length());
		}

		return result;
	}

	private void getUniformLocations(String shaderSrc) {
		HashMap<String, ArrayList<GLSLStructElement>> structs = this.findUniformStructs(shaderSrc);

		final String UNIFORM_KEYWORD = new String("uniform");

		int uniformStartLocation = shaderSrc.indexOf(UNIFORM_KEYWORD);
		while (uniformStartLocation != -1) {
			if (uniformStartLocation == 0 || !Character.isWhitespace(shaderSrc.charAt(uniformStartLocation - 1)) && shaderSrc.charAt(uniformStartLocation - 1) != ';'
				|| !Character.isWhitespace(shaderSrc.charAt(uniformStartLocation + UNIFORM_KEYWORD.length()))) {
				continue;
			}

			int startIndex = uniformStartLocation + UNIFORM_KEYWORD.length() + 1;
			int endIndex = shaderSrc.indexOf(";", startIndex);

			String uniformLine = shaderSrc.substring(startIndex, endIndex);

			String uniformType = uniformLine.substring(0, uniformLine.indexOf(" ")).trim();
			String uniformName = uniformLine.substring(uniformLine.indexOf(" ") + 1, uniformLine.length()).trim();

			this.addUniform(uniformType, uniformName, structs);

			uniformStartLocation = shaderSrc.indexOf(UNIFORM_KEYWORD, uniformStartLocation + UNIFORM_KEYWORD.length());
		}
	}

	private void addUniform(String uniformType, String uniformName, HashMap<String, ArrayList<GLSLStructElement>> structs) {
		ArrayList<GLSLStructElement> structElements = structs.get(uniformType);

		if (structElements != null) {
			for (GLSLStructElement structElement : structElements) {
				this.addUniform(structElement.type, uniformName + "." + structElement.name, structs);
			}
		} else if (!uniforms.containsKey(uniformName)) {
			int uniformLocation = GL20.glGetUniformLocation(id, uniformName);
			if (uniformLocation == -1) {
				System.err.println("ERROR! Could not find location for uniform: " + uniformName);
				System.exit(1);
			}

			uniforms.put(uniformName, uniformLocation);
		}
	}

	public void bindShader() {
		GL20.glUseProgram(id);
	}

	public void unbindShader() {
		GL20.glUseProgram(0);
	}

	public void bindAttribLocation(int index, String name) {
		GL20.glBindAttribLocation(id, index, name);
	}

	public void setUniform(String name, int value) {
		GL20.glUniform1i(uniforms.get(name), value);
	}

	public void setUniform(String name, float value) {
		GL20.glUniform1f(uniforms.get(name), value);
	}

	public void setUniform(String name, Vector2f value) {
		GL20.glUniform2f(uniforms.get(name), value.getX(), value.getY());
	}

	public void setUniform(String name, org.lwjgl.util.vector.Vector2f value) {
		GL20.glUniform2f(uniforms.get(name), value.getX(), value.getY());
	}

	public void setUniform(String name, Vector3f value) {
		GL20.glUniform3f(uniforms.get(name), value.getX(), value.getY(), value.getZ());
	}

	public void setUniform(String name, Vector4f value) {
		GL20.glUniform4f(uniforms.get(name), value.getX(), value.getY(), value.getZ(), value.getW());
	}

	public void setUniform(String name, Matrix4f value) {
		this.setUniform(name, value, false);
	}

	public void setUniform(String name, Matrix4f value, boolean transpose) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		value.store(buffer);
		buffer.flip();
		GL20.glUniformMatrix4(uniforms.get(name), transpose, buffer);
	}
}
