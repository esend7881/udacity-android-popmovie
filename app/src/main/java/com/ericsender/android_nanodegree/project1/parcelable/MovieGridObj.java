package com.ericsender.android_nanodegree.project1.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Eric K. Sender on 8/4/2015.
 * TODO: Use Getter/Setters
 */
public class MovieGridObj implements Parcelable {
    public String title;
    public Boolean adult;
    public String backdrop_path;
    public List<Double> genre_ids;
    public Double id;
    public String original_language;
    public String original_title;
    public String release_date;
    public String poster_path;
    public Double popularity;
    public Boolean video;
    public Double vote_average;
    public Double vote_count;

    public MovieGridObj() {}

    protected MovieGridObj(Parcel in) {
        title = in.readString();
        adult = Boolean.valueOf(in.readByte() == (byte) 1);
        backdrop_path = in.readString();
        genre_ids = in.readArrayList(ClassLoader.getSystemClassLoader());
        id = in.readDouble();
        original_language = in.readString();
        original_title = in.readString();
        release_date = in.readString();
        poster_path = in.readString();
        popularity = in.readDouble();
        video = Boolean.valueOf(in.readByte() == (byte) 1);
        vote_average = in.readDouble();
        vote_count = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeByte((byte) (adult ? 1 : 0));
        dest.writeString(backdrop_path);
        dest.writeList(genre_ids);
        dest.writeDouble(id);
        dest.writeString(original_language);
        dest.writeString(original_title);
        dest.writeString(release_date);
        dest.writeString(poster_path);
        dest.writeDouble(popularity);
        dest.writeByte((byte) (video ?  1 : 0));
        dest.writeDouble(vote_average);
        dest.writeDouble(vote_count);
    }

    public static final Creator<MovieGridObj> CREATOR = new Creator<MovieGridObj>() {
        @Override
        public MovieGridObj createFromParcel(Parcel in) {
            return new MovieGridObj(in);
        }

        @Override
        public MovieGridObj[] newArray(int size) {
            return new MovieGridObj[size];
        }
    };

    @Override
    public String toString() {
        return "MovieGridObj{" +
                "title='" + title + '\'' +
                ", adult=" + adult +
                ", backdrop_path='" + backdrop_path + '\'' +
                ", genre_ids=" + genre_ids +
                ", id=" + id +
                ", original_language='" + original_language + '\'' +
                ", original_title='" + original_title + '\'' +
                ", release_date='" + release_date + '\'' +
                ", poster_path='" + poster_path + '\'' +
                ", popularity=" + popularity +
                ", video=" + video +
                ", vote_average=" + vote_average +
                ", vote_count=" + vote_count +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }
}