/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.recipe;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;

public abstract class AbstractObjectGraphTest extends TestCase {
    public void testCreateSingle() {
        Repository repository = createNewRepository();
        ObjectGraph graph = new ObjectGraph(repository);

        Object actual = graph.create("Radiohead");
        assertNotNull("actual is null", actual);
        assertTrue("actual should be an instance of Artist", actual instanceof Artist);
        Artist radiohead = (Artist)actual;
        assertEquals(new Artist("Radiohead"), radiohead);

        // multiple calls should result in same object
        assertSame(radiohead, graph.create("Radiohead"));
    }

    public void testCreateNested() {
        Album expectedBends = createBends();

        Repository repository = createNewRepository();
        ObjectGraph graph = new ObjectGraph(repository);

        Object actual = graph.create("Bends");
        assertNotNull("actual is null", actual);
        assertTrue("actual should be an instance of Album", actual instanceof Album);
        Album actualBends = (Album)actual;
        assertEquals(expectedBends, actualBends);

        // we should get the same values out of the graph
        assertSame(actualBends, graph.create("Bends"));
        assertSame(actualBends.getArtist(), graph.create("Radiohead"));
        assertSame(actualBends.getSongs().get(0), graph.create("High and Dry"));
        assertSame(actualBends.getSongs().get(1), graph.create("Fake Plastic Trees"));
    }

    public void testCreateAll() {
        Album expectedBends = createBends();

        Repository repository = createNewRepository();
        ObjectGraph graph = new ObjectGraph(repository);

        Map<String,Object> created = graph.createAll("Bends");
        assertNotNull("created is null", created);
        assertEquals(4, created.size());

        Object actual = created.get("Bends");
        assertNotNull("actual is null", actual);
        assertTrue("actual should be an instance of Album", actual instanceof Album);
        Album actualBends = (Album)actual;
        assertEquals(expectedBends, actualBends);

        // we should get the same values out of the graph
        assertSame(actualBends, graph.create("Bends"));
        assertSame(created.get("Radiohead"), graph.create("Radiohead"));
        assertSame(created.get("High and Dry"), graph.create("High and Dry"));
        assertSame(created.get("Fake Plastic Trees"), graph.create("Fake Plastic Trees"));

        // verify nested objects in actualBends are the same objects in the created map
        assertSame(actualBends.getArtist(), created.get("Radiohead"));
        assertSame(actualBends.getSongs().get(0), created.get("High and Dry"));
        assertSame(actualBends.getSongs().get(1), created.get("Fake Plastic Trees"));
    }

    public void testCreateAllGroupings() {
        ObjectGraph graph = new ObjectGraph(createNewRepository());
        Map<String,Object> created = graph.createAll("Radiohead");
        assertEquals(Arrays.asList("Radiohead"), new ArrayList<String>(created.keySet()));

        graph = new ObjectGraph(createNewRepository());
        created = graph.createAll("Fake Plastic Trees");
        assertEquals(Arrays.asList("Radiohead", "Fake Plastic Trees"), new ArrayList<String>(created.keySet()));

        graph = new ObjectGraph(createNewRepository());
        created = graph.createAll("Fake Plastic Trees", "Fake Plastic Trees", "Fake Plastic Trees");
        assertEquals(Arrays.asList("Radiohead", "Fake Plastic Trees"), new ArrayList<String>(created.keySet()));

        graph = new ObjectGraph(createNewRepository());
        created = graph.createAll("Fake Plastic Trees", "Radiohead");
        assertEquals(Arrays.asList("Radiohead", "Fake Plastic Trees"), new ArrayList<String>(created.keySet()));

        graph = new ObjectGraph(createNewRepository());
        created = graph.createAll("Bends");
        assertEquals(Arrays.asList("Radiohead", "High and Dry", "Fake Plastic Trees", "Bends"), new ArrayList<String>(created.keySet()));

        graph = new ObjectGraph(createNewRepository());
        created = graph.createAll("Radiohead");
        assertEquals(Arrays.asList("Radiohead"), new ArrayList<String>(created.keySet()));
        created = graph.createAll("Bends");
        assertEquals(Arrays.asList("High and Dry", "Fake Plastic Trees", "Bends"), new ArrayList<String>(created.keySet()));
    }

    public void testCreateUnknown() {
        ObjectGraph graph = new ObjectGraph(createNewRepository());
        try {
            graph.create("Unknown");
            fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {
            assertEquals(expected.getName(), "Unknown");
        }

        try {
            graph.createAll("Unknown");
            fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {
            assertEquals(expected.getName(), "Unknown");
        }

        try {
            graph.createAll("Radiohead", "Unknown");
            fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {
            assertEquals(expected.getName(), "Unknown");
        }

        try {
            graph.createAll("Unknown", "Radiohead");
            fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {
            assertEquals(expected.getName(), "Unknown");
        }
    }

    public void testCircularDependency() {
        // Add a constructor dependency
        Repository repository = createNewRepository();
        ObjectRecipe recipe = (ObjectRecipe) repository.get("Radiohead");
        recipe.setConstructorArgNames(new String[] {"name", "albums"});
        recipe.setProperty("albums", new CollectionRecipe(Arrays.asList(new ReferenceRecipe("Bends"))));

        ObjectGraph graph = new ObjectGraph(repository);
        try {
            graph.create("Bends");
            fail("Expected CircularDependencyException");
        } catch (CircularDependencyException expected) {
            assertCircularity(Arrays.asList(repository.get("Bends"), repository.get("Radiohead"), repository.get("Bends")),
                    expected.getCircularDependency());
        }

        // Add a non constructor dependency
        // This will fail too, unless Option.LAZY_ASSIGNMENT is set
        repository = createNewRepository();
        recipe = (ObjectRecipe) repository.get("Radiohead");
        recipe.setConstructorArgNames(new String[] {"name"});
        recipe.setProperty("albums", new CollectionRecipe(Arrays.asList(repository.get("Bends"))));

        graph = new ObjectGraph(repository);
        try {
            graph.create("Bends");
            fail("Expected CircularDependencyException");
        } catch (CircularDependencyException expected) {
            assertCircularity(Arrays.asList(repository.get("Bends"), repository.get("Radiohead"), repository.get("Bends")),
                    expected.getCircularDependency());
        }
    }

    public void testInvalidGraph() {
        Repository repository = createNewRepository();
        ObjectRecipe bends = (ObjectRecipe) repository.get("Bends");
        bends.setName("Other");

        ObjectGraph graph = new ObjectGraph(repository);
        try {
//            graph.create("Bends");
//            fail("Expected ConstructionException");
        } catch (ConstructionException expected) {
        }

        //
        // Multiple recipies with the same name
        //
        repository = createNewRepository();

        ObjectRecipe radiohead = new ObjectRecipe(Artist.class, new String[]{"name"});
        radiohead.setName("Radiohead");
        radiohead.setProperty("name", "Radiohead");
        repository.add("Radiohead", radiohead);
        bends = (ObjectRecipe) repository.get("Bends");
        bends.setProperty("artist", radiohead);

        radiohead = new ObjectRecipe(Artist.class, new String[]{"name"});
        radiohead.setName("Radiohead");
        radiohead.setProperty("name", "Radiohead");
        repository.add("Radiohead", radiohead);
        ObjectRecipe highAndDry = (ObjectRecipe) repository.get("High and Dry");
        highAndDry.setProperty("composer", radiohead);

        graph = new ObjectGraph(repository);
        try {
            graph.create("Bends");
            fail("Expected ConstructionException");
        } catch (ConstructionException expected) {
        }
    }

    protected abstract Repository createNewRepository();

    private Album createBends() {
        Artist radiohead = new Artist("Radiohead");
        Album bends = new Album("Bends", radiohead);
        Song highAndDry = new Song("High and Dry", radiohead);
        Song fakePlasticTrees = new Song("Fake Plastic Trees", radiohead);

        bends.setSongs(Arrays.asList(highAndDry, fakePlasticTrees));

        return bends;
    }

    public static void assertCircularity(List<?> expected, List<?> actual) {
//        if (expected.size() != actual.size()) {
//            // this will fail, with nice message
//            assertEquals(expected, actual);
//        }

        List<Object> newActual = new ArrayList<Object>(actual.subList(0, actual.size() -1));

        Object start = expected.get(0);
        int index = actual.indexOf(start);
        if (index > 0) Collections.rotate(newActual, index);
        newActual.add(start);


        assertEquals(expected, newActual);
//
//
//        if (index < 1) {
//            // acutal list already starts at the same object as the expected list
//            assertEquals(expected, actual);
//        } else {
//            // create a new actual list rotated at the
////            List<Object> newActual = new ArrayList<Object>(actual.size());
////            newActual.addAll(actual.subList(index, actual.size()));
////            newActual.addAll(actual.subList(1, index));
////            newActual.add(start);
////            assertEquals(expected, newActual);
//        }
    }

    public static class Album {
        private final String name;
        private final Artist artist;
        private List<Song> songs;

        public Album(String name, Artist artist) {
            if (name == null) throw new NullPointerException("name is null");
            if (artist == null) throw new NullPointerException("artist is null");
            this.name = name;
            this.artist = artist;
        }

        public String getName() {
            return name;
        }

        public Artist getArtist() {
            return artist;
        }

        public List<Song> getSongs() {
            return songs;
        }

        public void setSongs(List<Song> songs) {
            this.songs = songs;
        }

        public String toString() {
            return "[Album: " + name + " - " + artist.getName() + "]";
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Album album = (Album) o;

            return name.equals(album.name) &&
                    artist.equals(album.artist) &&
                    !(songs != null ? !songs.equals(album.songs) : album.songs != null);

        }

        public int hashCode() {
            int result;
            result = name.hashCode();
            result = 31 * result + artist.hashCode();
            result = 31 * result + (songs != null ? songs.hashCode() : 0);
            return result;
        }
    }

    public static class Song {
        private final String name;
        private final Artist composer;

        public Song(String name, Artist composer) {
            if (name == null) throw new NullPointerException("name is null");
            if (composer == null) throw new NullPointerException("composer is null");
            this.name = name;
            this.composer = composer;
        }

        public String getName() {
            return name;
        }

        public Artist getComposer() {
            return composer;
        }

        public String toString() {
            return "[Song: " + name + " - " + composer.name + "]";
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Song song = (Song) o;

            return name.equals(song.name) && composer.equals(song.composer);
        }

        public int hashCode() {
            int result;
            result = name.hashCode();
            result = 31 * result + composer.hashCode();
            return result;
        }
    }

    public static class Artist {
        private final String name;
        private final Set<Album> albums = new HashSet<Album>();

        public Artist(String name) {
            if (name == null) throw new NullPointerException("name is null");
            this.name = name;
        }

        public Artist(String name, Set<Album> albums) {
            if (name == null) throw new NullPointerException("name is null");
            this.name = name;
            this.albums.addAll(albums);
        }

        public String getName() {
            return name;
        }

        public Set<Album> getAlbums() {
            return albums;
        }

        public void setAlbums(Set<Album> albums) {
            this.albums.clear();
            this.albums.addAll(albums);
        }

        public String toString() {
            return "[Artist: " + name + "]";
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Artist artist = (Artist) o;

            return name.equals(artist.name);
        }

        public int hashCode() {
            return name.hashCode();
        }
    }
}
