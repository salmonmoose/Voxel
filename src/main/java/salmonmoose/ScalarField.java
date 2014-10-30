package salmonmoose;

import salmonmoose.glm.*;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;
import static org.lwjgl.opengl.GL33.*;

public class ScalarField {

	private int vaoId = 0;
	private int vboId = 0;
	private int vboiId = 0;
	private int indicesCount = 0;

	private TexturedVertex[] vertices = null;
	private ByteBuffer vertexByteBuffer = null;
	private ByteBuffer verticesByteBuffer = null;

	public ScalarField() {
		setupQuad();
	}

	public void render() {
		// Bind to the VAO that has all the information about the vertices
		GL30.glBindVertexArray(vaoId);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		
		// Bind to the index VBO that has all the information about the order of the vertices
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);
		
		// Draw the vertices
		GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0);
		
		// Put everything back to default (deselect)
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL30.glBindVertexArray(0);
	}

	private void setupQuad() {
		// We'll define our quad using 4 vertices of the custom 'TexturedVertex' class
		TexturedVertex v0 = new TexturedVertex(); 
		v0.setXYZ(-0.5f, -0.5f, -0.5f);
		v0.setRGB(0, 0, 0);
		v0.setST(0, 0);
		
		TexturedVertex v1 = new TexturedVertex();
		v1.setXYZ(-0.5f, -0.5f, 0.5f);
		v1.setRGB(0, 0, 1);
		v1.setST(0, 0);

		TexturedVertex v2 = new TexturedVertex();
		v2.setXYZ(-0.5f, 0.5f, -0.5f);
		v2.setRGB(0, 1, 0);
		v2.setST(0, 0);

		TexturedVertex v3 = new TexturedVertex();
		v3.setXYZ(-0.5f, 0.5f, 0.5f);
		v3.setRGB(0, 1, 1);
		v3.setST(0, 0);		

		TexturedVertex v4 = new TexturedVertex(); 
		v4.setXYZ(0.5f, -0.5f, -0.5f);
		v4.setRGB(1, 0, 0);
		v4.setST(0, 0);
		
		TexturedVertex v5 = new TexturedVertex();
		v5.setXYZ(0.5f, -0.5f, 0.5f);
		v5.setRGB(1, 0, 1);
		v5.setST(0, 0);

		TexturedVertex v6 = new TexturedVertex();
		v6.setXYZ(0.5f, 0.5f, -0.5f);
		v6.setRGB(1, 1, 0);
		v6.setST(0, 0);

		TexturedVertex v7 = new TexturedVertex();
		v7.setXYZ(0.5f, 0.5f, 0.5f);
		v7.setRGB(1, 1, 1);
		v7.setST(0, 0);
		
		vertices = new TexturedVertex[] {v0, v1, v2, v3, v4, v5, v6, v7};
		
		// Create a FloatBufer of the appropriate size for one vertex
		vertexByteBuffer = BufferUtils.createByteBuffer(TexturedVertex.stride);
		
		// Put each 'Vertex' in one FloatBuffer
		verticesByteBuffer = BufferUtils.createByteBuffer(vertices.length * 
				TexturedVertex.stride);				
		FloatBuffer verticesFloatBuffer = verticesByteBuffer.asFloatBuffer();
		for (int i = 0; i < vertices.length; i++) {
			// Add position, color and texture floats to the buffer
			verticesFloatBuffer.put(vertices[i].getElements());
		}
		verticesFloatBuffer.flip();
		
		
		// OpenGL expects to draw vertices in counter clockwise order by default
		byte[] indices = {
				0, 3, 1, //x -1
				0, 2, 3,
				4, 5, 7, //x +1
				4, 7, 6,
				0, 5, 4, //y -1
				0, 1, 5,
				2, 6, 7, //y +1
				2, 7, 3,
				2, 0, 4, //z -1
				2, 4, 6,
				3, 5, 1, //z +1
				3, 7, 5

		};
		indicesCount = indices.length;
		ByteBuffer indicesBuffer = BufferUtils.createByteBuffer(indicesCount);
		indicesBuffer.put(indices);
		indicesBuffer.flip();
		
		// Create a new Vertex Array Object in memory and select it (bind)
		vaoId = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoId);
		
		// Create a new Vertex Buffer Object in memory and select it (bind)
		vboId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesFloatBuffer, GL15.GL_STREAM_DRAW);
		
		// Put the position coordinates in attribute list 0
		GL20.glVertexAttribPointer(0, TexturedVertex.positionElementCount, GL11.GL_FLOAT, 
				false, TexturedVertex.stride, TexturedVertex.positionByteOffset);
		// Put the color components in attribute list 1
		GL20.glVertexAttribPointer(1, TexturedVertex.colorElementCount, GL11.GL_FLOAT, 
				false, TexturedVertex.stride, TexturedVertex.colorByteOffset);
		// Put the texture coordinates in attribute list 2
		GL20.glVertexAttribPointer(2, TexturedVertex.textureElementCount, GL11.GL_FLOAT, 
				false, TexturedVertex.stride, TexturedVertex.textureByteOffset);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		// Deselect (bind to 0) the VAO
		GL30.glBindVertexArray(0);
		
		// Create a new VBO for the indices and select it (bind) - INDICES
		vboiId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
}