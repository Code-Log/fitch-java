/*

A platformer game written using OpenGL.
    Copyright (C) 2017-2018  Jaco Malan

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package com.codelog.fitch.game;

import com.codelog.fitch.Main;
import com.codelog.fitch.graphics.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.Matrix4;
import com.codelog.fitch.math.Vector2;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;

import java.io.IOException;

import static com.codelog.fitch.graphics.Texture2D.loadTexture;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class Player implements Drawable {

    private Vector2 pos;
    private float width;
    private float height;
    private boolean isStanding = false;
    private boolean isRunning = false;
    private float drawDepth = 0f;
    private Texture2D texture;
    private Body body;

    private ShaderProgram shaderProgram;
    private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private MatrixStack<Matrix4> matrixStack;

    public Player(Vector2 pos, float width, float height) {
        this.pos = pos;
        this.width = width;
        this.height = height;
    }

    public Player(Rectangle rect) {
        new Player(new Vector2(rect.getX(), rect.getY()), rect.getWidth(), rect.getHeight());
    }

    private void setupBuffers(GL4 gl) {

        float x = (float)pos.x;
        float y = (float)pos.y;

        float[] vertices = {
                x,          y,            drawDepth, 0, 0,
                x,          y + height,   drawDepth, 0, 1,
                x + width,  y,            drawDepth, 1, 0,
                x + width,  y + height,   drawDepth, 1, 1,
        };

        vbo.sendFloatData(gl, vertices, gl.GL_DYNAMIC_DRAW);

    }

    @Override
    public void update(GL4 gl) {

        setupBuffers(gl);

        Matrix4 ident = new Matrix4();
        matrixStack.push(ident);

        this.pos = Main.worldToPixels(body.getPosition());

    }

    @Override
    public void init(GL4 gl) {

        vao = new VertexArrayObject(gl);
        vao.bind(gl);
        vbo = new VertexBufferObject(gl, gl.GL_ARRAY_BUFFER);
        vbo.bind(gl);
        shaderProgram = new ShaderProgram(gl);
        shaderProgram.addVertexShader("shaders/pvshader.glsl");
        shaderProgram.addFragmentShader("shaders/pfshader.glsl");

        try {
            shaderProgram.compile(gl);
        } catch (ShaderCompilationException | IOException e) {
            Main.getLogger().log(this, e);
        }

        setupBuffers(gl);

        matrixStack = new MatrixStack<>();

        try {
            texture = loadTexture(gl, "player.png");
        } catch (IOException e) {
            Main.getLogger().log(this, e);
        }

        // Init Box2D body
        var bodyDef = new BodyDef();
        bodyDef.position = Main.pixelsToWorld(pos);
        bodyDef.type = BodyType.DYNAMIC;
        bodyDef.fixedRotation = true;

        var shape = new PolygonShape();
        shape.setAsBox((float)Main.scalarPToW(width) / 2f, (float)Main.scalarPToW(height) / 2f);

        var fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;

        body = Main.world.createBody(bodyDef);
        fixtureDef.density = 1;
        fixtureDef.friction = 0.3f;
        fixtureDef.restitution = 0f;
        body.createFixture(fixtureDef);

    }

    @Override
    @SuppressWarnings("Duplicates")
    public void draw(GL4 gl) {

        vao.bind(gl);
        vbo.bind(gl);
        shaderProgram.bind(gl);
        texture.bind(gl);

        Matrix4 projMat = MatrixStack.flattenStack(matrixStack);

        int handle = gl.glGetUniformLocation(shaderProgram.getID(), "projMat");
        gl.glUniformMatrix4fv(handle, 1, false, projMat.getMatrix(), 0);

        gl.glEnableVertexArrayAttrib(vao.getID(), 0);
        gl.glEnableVertexArrayAttrib(vao.getID(), 1);

        gl.glVertexAttribPointer(0, 3, gl.GL_FLOAT, false, 5 * Float.BYTES, 0);
        gl.glVertexAttribPointer(1, 2, gl.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        gl.glDrawArrays(gl.GL_TRIANGLE_STRIP, 0, 4);

        gl.glDisableVertexArrayAttrib(vao.getID(), 0);
        gl.glDisableVertexArrayAttrib(vao.getID(), 1);

    }

    public Vector2 getPos() { return pos; }
    public void setPos(Vector2 pos) { this.pos = pos; }

    public MatrixStack<Matrix4> getMatrixStack() { return matrixStack; }
    public void loadMatrixStack(MatrixStack<Matrix4> _mstack) { matrixStack = _mstack; }

    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public boolean getRunning() { return isRunning; }
    public boolean getStanding() { return isStanding; }

    public float getDrawDepth() { return drawDepth; }
    public void setDrawDepth(float _dd) { drawDepth = _dd; }

    public Body getBody() { return body; }

    public void setTexture(Texture2D texture, boolean changeDims) {
        this.texture = texture;
        if (changeDims) {
            this.width = texture.getWidth();
            this.height = texture.getHeight();
        }
    }
}
