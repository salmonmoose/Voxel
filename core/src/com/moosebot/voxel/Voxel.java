package com.moosebot.voxel;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Voxel extends ApplicationAdapter {
    public Environment lights;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public CameraInputController camController;
    public Model model;
    public Mesh mesh;
    public ModelInstance instance;
    public ShaderProgram shader;
    
    public static final String VERT_SHADER =
        "attribute vec4 a_position;    \n" + 
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" + 
        "uniform mat4 u_worldView;\n" + 
        "varying vec4 v_color;" + 
        "varying vec2 v_texCoords;" + 
        "void main()                  \n" + 
        "{                            \n" + 
        "   v_color = vec4(1, 1, 1, 1); \n" + 
        "   v_texCoords = a_texCoord0; \n" + 
        "   gl_Position =  u_worldView * a_position;  \n"      + 
        "}\n";

    public static final String FRAG_SHADER = 
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" + 
        "#endif\n" + 
        "varying vec4 v_color;\n" + 
        "varying vec2 v_texCoords;\n" + 
        "uniform sampler2D u_texture;\n" + 
        "void main()                                  \n" + 
        "{                                            \n" + 
        "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
        "}";

    @Override
    public void create () {
        lights = new Environment();
        lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        ModelBuilder modelBuilder = new ModelBuilder();

        model = modelBuilder.createBox(
            5f, 5f, 5f,
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal
        );

        instance = new ModelInstance(model);

        mesh = testMesh();
        shader = createMeshShader();
    }

    @Override
    public void render () {
        camController.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        //modelBatch.begin(cam);
        //modelBatch.render(instance, lights);
        //modelBatch.end();
        shader.begin();
        shader.setUniformMatrix("u_worldView", matrix);
        shader.setUniformi("u_texture", 0);
        mesh.render(shader, GL20.GL_TRIANGLES);

        shader.end();
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
    }

    public static ShaderProgram createMeshShader() {
        ShaderProgram.pedantic = false;
        ShaderProgram shader = new ShaderProgram(VERT_SHADER, FRAG_SHADER);
        String log = shader.getLog();

        if (!shader.isCompiled())
            throw new GdxRuntimeException(log);
        if (log != null && log.length() != 0)
            System.out.println("Shader Log: "+log);
        return shader;
    }

    public Mesh testMesh() {
        mesh = new Mesh(true, 4, 6, VertexAttribute.Position(), VertexAttribute.  ColorUnpacked(), VertexAttribute.TexCoords(0));
        
        mesh.setVertices(new float[] 
            {-0.5f, -0.5f, 0, 1, 1, 1, 1, 0, 1,
            0.5f, -0.5f, 0, 1, 1, 1, 1, 1, 1,
            0.5f, 0.5f, 0, 1, 1, 1, 1, 1, 0,
            -0.5f, 0.5f, 0, 1, 1, 1, 1, 0, 0}
        );

        mesh.setIndices(new short[] {0, 1, 2, 2, 3, 0});

        return mesh;
    }


}
