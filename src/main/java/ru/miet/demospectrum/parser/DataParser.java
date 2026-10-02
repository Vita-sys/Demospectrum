package ru.miet.demospectrum.parser;

import java.io.IOException;
import java.util.List;

public interface DataParser<T> {
    List<T> parse(String url, int cityId) throws IOException;
}