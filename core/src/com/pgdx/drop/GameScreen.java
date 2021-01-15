package com.pgdx.drop;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen implements Screen {
    final DropGame game;

    Texture dropImage;
    Texture bucketImage;
    Sound dropSound;
    Sound gameOverSound;
    Music rainMusic;
    OrthographicCamera camera;
    Rectangle bucket;
    Array<Rectangle> raindrops;
    long lastDropTime;
    int dropsGathered;
    private boolean gameOver;
    private Rectangle gameOverButton;
    Texture gameOverImage;
    Texture backgroundImage;
    Texture gameOverWallpaper;
    GlyphLayout gameOverTextL;

    public GameScreen(final DropGame game) {
        this.game = game;
        gameOver = false;
        dropsGathered = 0;

        game.setGameScreen(this); // Passing reference to DropGame for calling GameScreen dispose method.

        // load the images for the droplet and the bucket, 64x64 pixels each
        dropImage = new Texture(Gdx.files.internal("droplet.png"));
        bucketImage = new Texture(Gdx.files.internal("bucket.png"));
        backgroundImage = new Texture(Gdx.files.internal("wallpaper.jpg"));
        gameOverWallpaper = new Texture(Gdx.files.internal("gameOverWallpaper.jpg"));

        // load the drop sound effect and the rain background "music"
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"));
        rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
        rainMusic.setLooping(true);
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameOver.wav"));

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        // create a Rectangle to logically represent the bucket
        bucket = new Rectangle();
        bucket.x = 800 / 2 - 64 / 2; // center the bucket horizontally
        bucket.y = 20; // bottom left corner of the bucket is 20 pixels above
        // the bottom screen edge
        bucket.width = 64;
        bucket.height = 64;

        // create the raindrops array and spawn the first raindrop
        raindrops = new Array<Rectangle>();
        spawnRaindrop();

        gameOverTextL = new GlyphLayout(game.font, "");

    }

    private void spawnRaindrop() {
        Rectangle raindrop = new Rectangle();
        raindrop.x = MathUtils.random(0, 800 - 64);
        raindrop.y = 480;
        raindrop.width = 64;
        raindrop.height = 64;
        raindrops.add(raindrop);
        lastDropTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {

        if (gameOver)
        {
            Gdx.gl.glClearColor(0, 0, 0.2f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            game.batch.begin();
            game.batch.draw(gameOverWallpaper,0,0,camera.viewportWidth,camera.viewportHeight);
            game.font.draw(game.batch, gameOverTextL, (camera.viewportWidth - gameOverTextL.width) / 2, 150);
            game.batch.draw(gameOverImage, gameOverButton.x, gameOverButton.y, gameOverButton.width, gameOverButton.height);
            game.batch.end();

            if (Gdx.input.isTouched()) {
                Vector3 touchPos = new Vector3();
                touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);

                if (touchPos.x >= gameOverButton.x && touchPos.x <= (gameOverButton.x + gameOverButton.getWidth()))
                {
                    if (touchPos.y >= gameOverButton.y && touchPos.y <= (gameOverButton.y + gameOverButton.getHeight()))
                    {
                        if (gameOver)
                        {
                            this.reloadGame();
                        }
                    }
                }
            }
        }
        else{
            Gdx.gl.glClearColor(0, 0, 0.2f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // tell the camera to update its matrices.
            camera.update();

            // tell the SpriteBatch to render in the
            // coordinate system specified by the camera.
            game.batch.setProjectionMatrix(camera.combined);

            // begin a new batch and draw the bucket and
            // all drops
            game.batch.begin();
            game.batch.draw(backgroundImage,0,0,camera.viewportWidth,camera.viewportHeight);
            game.font.draw(game.batch, "Drops Collected: " + dropsGathered, 0, 480);
            game.font.draw(game.batch, "FPS: " + String.valueOf(Gdx.graphics.getFramesPerSecond()),0,440);
            game.batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height);
            for (Rectangle raindrop : raindrops) {
                game.batch.draw(dropImage, raindrop.x, raindrop.y);
            }
            game.batch.end();

            // process user input
            if (Gdx.input.isTouched()) {
                Vector3 touchPos = new Vector3();
                touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPos);
                bucket.x = touchPos.x - 64 / 2;
            }
            if (Gdx.input.isKeyPressed(Keys.LEFT))
                bucket.x -= 200 * Gdx.graphics.getDeltaTime();
            if (Gdx.input.isKeyPressed(Keys.RIGHT))
                bucket.x += 200 * Gdx.graphics.getDeltaTime();

            // make sure the bucket stays within the screen bounds
            if (bucket.x < 0)
                bucket.x = 0;
            if (bucket.x > 800 - 64)
                bucket.x = 800 - 64;

            // check if we need to create a new raindrop
            if (TimeUtils.nanoTime() - lastDropTime > 1000000000)
                spawnRaindrop();

            // move the raindrops, remove any that are beneath the bottom edge of
            // the screen or that hit the bucket. In the later case we increase the
            // value our drops counter and add a sound effect.
            Iterator<Rectangle> iter = raindrops.iterator();
            while (iter.hasNext()) {
                Rectangle raindrop = iter.next();
                raindrop.y -= 200 * Gdx.graphics.getDeltaTime();
                if (raindrop.y + 64 < 0)
                {
                    iter.remove();
                    gameOver();
                }

                float bucketMaxY = bucket.getY() + 64;
                if (raindrop.overlaps(bucket) && (bucketMaxY - raindrop.getY()) <= 15) {
                    dropsGathered++;
                    dropSound.play();
                    iter.remove();
                }
            }
        }



    }

    private void reloadGame() {
        gameOver = false;
        dropsGathered = 0;

        bucket.x = 800 / 2 - 64 / 2; // center the bucket horizontally
        bucket.y = 20; // bottom left corner of the bucket is 20 pixels above
        rainMusic.play();
        rainMusic.setLooping(true);
        raindrops = new Array<Rectangle>();
        spawnRaindrop();
    }

    private void gameOver() {
        rainMusic.stop();
        gameOverSound.play();
        gameOver = true;
        if (gameOverButton == null)
        {
            gameOverButton = new Rectangle();

            gameOverButton.y = 20;


        }
        if (gameOverImage == null)
        {
            gameOverImage = new Texture(Gdx.files.internal("retry.png"));
            gameOverButton.width =  gameOverImage.getWidth();
            gameOverButton.height = gameOverImage.getHeight();

            gameOverButton.x = 800 / 2 - gameOverButton.width / 2;
        }

        gameOverTextL.setText(game.font, "Drops Gathered: " + String.valueOf(dropsGathered));

    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        rainMusic.play();
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        dropImage.dispose();
        bucketImage.dispose();
        dropSound.dispose();
        rainMusic.dispose();

        gameOverImage.dispose();
        backgroundImage.dispose();
        gameOverWallpaper.dispose();
        gameOverSound.dispose();

    }

}
