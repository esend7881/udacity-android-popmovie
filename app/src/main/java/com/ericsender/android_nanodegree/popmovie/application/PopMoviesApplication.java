package com.ericsender.android_nanodegree.popmovie.application;

import android.app.Application;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Eric on 9/11/2015.
 */
public class PopMoviesApplication extends Application {
    public static final State STATE = State.getInstance();

    public static class State {
        private final AtomicBoolean isRefreshGrid = new AtomicBoolean();
        private final AtomicBoolean isTwoPane = new AtomicBoolean();
        private final AtomicBoolean detailsPaneShown = new AtomicBoolean();
        private String currSortState;

        public void setTwoPane(boolean b) {
            isTwoPane.set(b);
        }

        public void setCurrSortState(String s) {
            currSortState = s;
        }

        public String getCurrSortState() {
            return currSortState;
        }

        public boolean getTwoPane() {
            return isTwoPane.get();
        }

        public boolean getIsRefreshGrid() {
            return isRefreshGrid.get();
        }

        public void setIsRefreshGrid(boolean b) {
            isRefreshGrid.set(b);
        }

        public void setDetailsPaneShown(boolean detailsPaneShown) {
            this.detailsPaneShown.set(detailsPaneShown);
        }

        public boolean isDetailsPaneShown() {
            return detailsPaneShown.get();
        }

        private static class SingletonHolder {
            private static final State INSTANCE = new State();
        }

        public static State getInstance() {
            return SingletonHolder.INSTANCE;
        }

        private State() {
        }
    }

}