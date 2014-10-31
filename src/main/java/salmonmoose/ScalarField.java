package salmonmoose;

import salmonmoose.glm.*;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.Collections;

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

	private static final int X_SIZE = 16;
	private static final int Y_SIZE = 16;
	private static final int Z_SIZE = 16;
	private static final int SIZE = X_SIZE * Y_SIZE * Z_SIZE;

	private TexturedVertex[] vertices = null;
	private ArrayList<TexturedVertex> vertexList = new ArrayList<TexturedVertex>();
	private ArrayList<Integer> indexList = new ArrayList<Integer>();
	private ByteBuffer vertexByteBuffer = null;
	private ByteBuffer verticesByteBuffer = null;
	private IntBuffer vertexIntBuffer = null;
	private IntBuffer verticesIntBuffer = null;

	private float[] field = null;

	public ScalarField() {
		setupField();

		setupBoxel();
	}

	public static int partBy1(int n) {
        n&= 0x0000ffff;

        n = (n | (n << 8)) & 0x00FF00FF;
        n = (n | (n << 4)) & 0x0F0F0F0F;
        n = (n | (n << 2)) & 0x33333333;
        n = (n | (n << 1)) & 0x55555555;
        return n;
	}

	public static int partBy2(int n) {
        n&= 0x000003ff;

        n = (n ^ (n << 16)) & 0xff0000ff;
        n = (n ^ (n <<  8)) & 0x0300f00f;
        n = (n ^ (n <<  4)) & 0x030c30c3;
        n = (n ^ (n <<  2)) & 0x09249249;
        
        return n;
	}

	public static int zipBy1(int n) {
    	n&= 0x55555555;

        n = (n ^ (n >> 1)) & 0x33333333;
        n = (n ^ (n >> 2)) & 0x0f0f0f0f;
        n = (n ^ (n >> 4)) & 0x00ff00ff;
        n = (n ^ (n >> 8)) & 0x0000ffff;
        
        return n;
	}

	public static int zipBy2(int n) {
		n&= 0x09249249;

        n = (n ^ (n >>  2)) & 0x030c30c3;
        n = (n ^ (n >>  4)) & 0x0300f00f;
        n = (n ^ (n >>  8)) & 0xff0000ff;
        n = (n ^ (n >> 16)) & 0x000003ff;
        
        return n;
	}

	public static int PosToIndex(int x, int y, int z) {
		return partBy2(x) | (partBy2(y) << 1) | (partBy2(z) << 2);
	}

	public static int IndexToPosX(int index) {
		return zipBy2(index);
	}

	public static int IndexToPosY(int index) {
		return zipBy2(index >> 1);
	}

	public static int IndexToPosZ(int index) {
		return zipBy2(index >> 2);
	}

	public void setupField() {
		field = new float[SIZE];

		for (int z = 0; z < Z_SIZE; z++) {
			for (int y = 0; y < Y_SIZE; y++) {
				for (int x = 0; x < X_SIZE; x++) {
					field[PosToIndex(x,y,z)] = (float)Math.random();
				}
			}
		}
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
		GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_INT, 0);
		
		// Put everything back to default (deselect)
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL30.glBindVertexArray(0);
	}

	private void setupBoxel() {
		for (int z = 0; z < Z_SIZE; z++) {
			for (int y = 0; y < Y_SIZE; y++) {
				for (int x = 0; x < X_SIZE; x++) {
					//if value is above .5, fill.
					if (field[PosToIndex(x,y,z)] > 0.5f) {
						makeCube((float) x, (float) y, (float) z);
					}
				}
			}
		}
		setupVBO();
	}

	private void setupVBO() {
		vertexByteBuffer = BufferUtils.createByteBuffer(TexturedVertex.stride);
		verticesByteBuffer = BufferUtils.createByteBuffer(vertexList.size() * TexturedVertex.stride);

		FloatBuffer verticesFloatBuffer = verticesByteBuffer.asFloatBuffer();

		for (int i = 0; i < vertexList.size(); i++) {
			// Add position, color and texture floats to the buffer
			verticesFloatBuffer.put(vertexList.get(i).getElements());
		}
		verticesFloatBuffer.flip();

		indicesCount = indexList.size();

		IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indicesCount);
		for (int i = 0; i < indexList.size(); i++) {
			indicesBuffer.put(indexList.get(i));
		}

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
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer.capacity() * 4, GL15.GL_STATIC_DRAW);
		GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, indicesBuffer);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private void makeCube(float offsetX, float offsetY, float offsetZ) {
		TexturedVertex vTemp = new TexturedVertex();
		int indexOffset = vertexList.size();

		vTemp.setXYZ(-0.5f + offsetX, -0.5f + offsetY, -0.5f + offsetZ);
		vTemp.setRGB(0, 0, 0);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();				
		vTemp.setXYZ(-0.5f + offsetX, -0.5f + offsetY, 0.5f + offsetZ);
		vTemp.setRGB(0, 0, 1);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);
		
		vTemp = new TexturedVertex();
		vTemp.setXYZ(-0.5f + offsetX, 0.5f + offsetY, -0.5f + offsetZ);
		vTemp.setRGB(0, 1, 0);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();
		vTemp.setXYZ(-0.5f + offsetX, 0.5f + offsetY, 0.5f + offsetZ);
		vTemp.setRGB(0, 1, 1);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();
		vTemp.setXYZ(0.5f + offsetX, -0.5f + offsetY, -0.5f + offsetZ);
		vTemp.setRGB(1, 0, 0);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();
		vTemp.setXYZ(0.5f + offsetX, -0.5f + offsetY, 0.5f + offsetZ);
		vTemp.setRGB(1, 0, 1);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();
		vTemp.setXYZ(0.5f + offsetX, 0.5f + offsetY, -0.5f + offsetZ);
		vTemp.setRGB(1, 1, 0);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		vTemp = new TexturedVertex();
		vTemp.setXYZ(0.5f + offsetX, 0.5f + offsetY, 0.5f + offsetZ);
		vTemp.setRGB(1, 1, 1);
		vTemp.setST(0, 0);
		vertexList.add(vTemp);

		Collections.addAll(indexList,
			0 + indexOffset, 3 + indexOffset, 1 + indexOffset, //x -1
			0 + indexOffset, 2 + indexOffset, 3 + indexOffset,
			4 + indexOffset, 5 + indexOffset, 7 + indexOffset, //x +1
			4 + indexOffset, 7 + indexOffset, 6 + indexOffset,
			0 + indexOffset, 5 + indexOffset, 4 + indexOffset, //y -1
			0 + indexOffset, 1 + indexOffset, 5 + indexOffset,
			2 + indexOffset, 6 + indexOffset, 7 + indexOffset, //y +1
			2 + indexOffset, 7 + indexOffset, 3 + indexOffset,
			2 + indexOffset, 0 + indexOffset, 4 + indexOffset, //z -1
			2 + indexOffset, 4 + indexOffset, 6 + indexOffset,
			3 + indexOffset, 5 + indexOffset, 1 + indexOffset, //z +1
			3 + indexOffset, 7 + indexOffset, 5 + indexOffset
		);
	}
}