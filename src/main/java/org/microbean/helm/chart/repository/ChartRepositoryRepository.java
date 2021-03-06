/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.helm.chart.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.regex.Pattern;

import hapi.chart.ChartOuterClass.Chart;

import org.microbean.development.annotation.Experimental;

import org.yaml.snakeyaml.Yaml;

import org.microbean.helm.chart.resolver.AbstractChartResolver;
import org.microbean.helm.chart.resolver.ChartResolverException;

/**
 * A repository of {@link ChartRepository} instances, normally built
 * from a Helm {@code repositories.yaml} file.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@Experimental
public class ChartRepositoryRepository extends AbstractChartResolver {


  /*
   * Static fields.
   */


  /**
   * A {@link Pattern} that matches a single solidus ("{@code /}").
   *
   * <p>This field is never {@code null}.</p>
   */
  private static final Pattern slashPattern = Pattern.compile("/");


  /*
   * Instance fields.
   */


  /**
   * The ({@linkplain Collections#unmodifiableSet(Set) immutable})
   * {@link Set} of {@link ChartRepository} instances managed by this
   * {@link ChartRepositoryRepository}.
   *
   * <p>This field is never {@code null}.</p>
   *
   * @see #ChartRepositoryRepository(Set)
   *
   * @see #getChartRepositories()
   */
  private final Set<ChartRepository> chartRepositories;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link ChartRepositoryRepository}.
   *
   * @param chartRepositories the {@link Set} of {@link
   * ChartRepository} instances to be managed by this {@link
   * ChartRepositoryRepository}; may be {@code null}; copied by value
   *
   * @see #getChartRepositories()
   */
  public ChartRepositoryRepository(final Set<? extends ChartRepository> chartRepositories) {
    super();
    if (chartRepositories == null || chartRepositories.isEmpty()) {
      this.chartRepositories = Collections.emptySet();
    } else {
      this.chartRepositories = Collections.unmodifiableSet(new LinkedHashSet<>(chartRepositories));
    }
  }


  /*
   * Instance methods.
   */


  /**
   * Returns the non-{@code null} {@linkplain
   * Collections#unmodifiableSet(Set) immutable} {@link Set} of {@link
   * ChartRepository} instances managed by this {@link
   * ChartRepositoryRepository}.
   *
   * @return the non-{@code null} {@linkplain
   * Collections#unmodifiableSet(Set) immutable} {@link Set} of {@link
   * ChartRepository} instances managed by this {@link
   * ChartRepositoryRepository}
   */
  public final Set<ChartRepository> getChartRepositories() {
    return this.chartRepositories;
  }

  /**
   * Returns the {@link ChartRepository} managed by this {@link
   * ChartRepositoryRepository} with the supplied {@code name}, or
   * {@code null} if there is no such {@link ChartRepository}.
   *
   * @param name the {@linkplain ChartRepository#getName() name} of
   * the {@link ChartRepository} to return; must not be {@code null}
   *
   * @return the {@link ChartRepository} managed by this {@link
   * ChartRepositoryRepository} with the supplied {@code name}, or
   * {@code null}
   *
   * @exception NullPointerException if {@code name} is {@code null}
   */
  public ChartRepository getChartRepository(final String name) {
    Objects.requireNonNull(name);
    ChartRepository returnValue = null;
    final Collection<? extends ChartRepository> repos = this.getChartRepositories();
    if (repos != null && !repos.isEmpty()) {
      for (final ChartRepository repo : repos) {
        if (repo != null && name.equals(repo.getName())) {
          returnValue = repo;
        }
      }
    }
    return returnValue;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation splits the supplied slash-delimited {@code
   * chartName} into a <em>chart repository name</em> and a <em>chart
   * name</em>, uses the chart repository name to {@linkplain
   * #getChartRepository(String) locate a suitable
   * <code>ChartRepository</code>}, and then calls {@link
   * ChartRepository#resolve(String, String)} with the chart name and
   * the supplied {@code chartVersion}, and returns the result.</p>
   *
   * @param chartName a slash-separated {@link String} whose first
   * component is a {@linkplain ChartRepository#getName() chart
   * repository name} and whose second component is a Helm chart name;
   * must not be {@code null}
   *
   * @param chartVersion the version of the chart to resolve; may be
   * {@code null} in which case "latest" semantics are implied
   *
   * @return a {@link Chart.Builder}, or {@code null}
   *
   * @see #resolve(String, String, String)
   */
  @Override
  public Chart.Builder resolve(final String chartName, final String chartVersion) throws ChartResolverException {
    Objects.requireNonNull(chartName);
    Chart.Builder returnValue = null;
    final String[] parts = slashPattern.split(chartName, 2);
    if (parts != null && parts.length == 2) {
      returnValue = this.resolve(parts[0], parts[1], chartVersion);
    }
    return returnValue;
  }

  /**
   * Uses the supplied {@code repositoryName}, {@code chartName} and
   * {@code chartVersion} parameters to find an appropriate Helm chart
   * and returns it in the form of a {@link Chart.Builder} object.
   *
   * <p>This implementation uses the supplied {@code repositoryName}
   * to {@linkplain #getChartRepository(String) locate a suitable
   * <code>ChartRepository</code>}, and then calls {@link
   * ChartRepository#resolve(String, String)} with the chart name and
   * the supplied {@code chartVersion}, and returns the result.</p>
   *
   * @param repositoryName a {@linkplain ChartRepository#getName()
   * chart repository name}; must not be {@code null}
   *
   * @param chartName a Helm chart name; must not be {@code null}
   *
   * @param chartVersion the version of the Helm chart to select; may
   * be {@code null} in which case "latest" semantics are implied
   *
   * @return a {@link Chart.Builder}, or {@code null}
   *
   * @exception ChartResolverException if there was a problem with
   * resolution
   *
   * @exception NullPointerException if {@code repositoryName} or
   * {@code chartName} is {@code null}
   *
   * @see #getChartRepository(String)
   *
   * @see ChartRepository#getName()
   *
   * @see ChartRepository#resolve(String, String)
   */
  public Chart.Builder resolve(final String repositoryName, final String chartName, final String chartVersion) throws ChartResolverException {
    Objects.requireNonNull(repositoryName);
    Objects.requireNonNull(chartName);
    Chart.Builder returnValue = null;
    final ChartRepository repo = this.getChartRepository(repositoryName);
    if (repo != null) {
      final Chart.Builder candidate = repo.resolve(chartName, chartVersion);
      if (candidate != null) {
        returnValue = candidate;
      }
    }
    return returnValue;
  }


  /*
   * Static methods.
   */

  
  /**
   * Creates and returns a new {@link ChartRepositoryRepository} from
   * the contents of a {@code repositories.yaml} file typically
   * located in the {@code ~/.helm/repository} directory.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return a new {@link ChartRepositoryRepository}; never {@code
   * null}
   *
   * @exception IOException if there was a problem reading the file
   *
   * @exception URISyntaxException if there was an invalid URI in the
   * file
   *
   * @see #fromYaml(InputStream, Path, Path)
   */
  public static final ChartRepositoryRepository fromHelmRepositoriesYaml() throws IOException, URISyntaxException {
    try (final InputStream stream = new BufferedInputStream(Files.newInputStream(ChartRepository.getHelmHome().resolve("repository/repositories.yaml")))) {
      return fromYaml(stream);
    }
  }

  /**
   * Creates and returns a new {@link ChartRepositoryRepository} from
   * the contents of a {@code repositories.yaml} file represented by
   * the supplied {@link InputStream}.
   *
   * @param stream the {@link InputStream} to read from; must not be
   * {@code null}
   *
   * @return a new {@link ChartRepositoryRepository}; never {@code
   * null}
   *
   * @exception IOException if there was a problem reading the file
   *
   * @exception URISyntaxException if there was an invalid URI in the
   * file
   *
   * @see #fromYaml(InputStream, Path, Path)
   */
  public static final ChartRepositoryRepository fromYaml(final InputStream stream) throws IOException, URISyntaxException {
    return fromYaml(stream, null, null);
  }

  /**
   * Creates and returns a new {@link ChartRepositoryRepository} from
   * the contents of a {@code repositories.yaml} file represented by
   * the supplied {@link InputStream}.
   *
   * @param stream the {@link InputStream} to read from; must not be
   * {@code null}
   *
   * @param archiveCacheDirectory an {@linkplain Path#isAbsolute()
   * absolute} {@link Path} representing a directory where Helm chart
   * archives may be stored; if {@code null} then a {@link Path}
   * beginning with the absolute directory represented by the value of
   * the {@code helm.home} system property, or the value of the {@code
   * HELM_HOME} environment variable, appended with {@code
   * cache/archive} will be used instead
   *
   * @param indexCacheDirectory an {@linkplain Path#isAbsolute()
   * absolute} {@link Path} representing a directory that the supplied
   * {@code cachedIndexPath} parameter value will be considered to be
   * relative to; will be ignored and hence may be {@code null} if the
   * supplied {@code cachedIndexPath} parameter value {@linkplain
   * Path#isAbsolute()}
   *
   * @return a new {@link ChartRepositoryRepository}; never {@code
   * null}
   *
   * @exception IOException if there was a problem reading the file
   *
   * @exception URISyntaxException if there was an invalid URI in the
   * file
   */
  public static final ChartRepositoryRepository fromYaml(final InputStream stream, Path archiveCacheDirectory, Path indexCacheDirectory) throws IOException, URISyntaxException {
    Objects.requireNonNull(stream);
    Path helmHome = null;
    if (archiveCacheDirectory == null) {
      helmHome = ChartRepository.getHelmHome();
      assert helmHome != null;
      archiveCacheDirectory = helmHome.resolve("cache/archive");
      assert archiveCacheDirectory != null;
    }
    if (!Files.isDirectory(archiveCacheDirectory)) {
      throw new IllegalArgumentException("!Files.isDirectory(archiveCacheDirectory): " + archiveCacheDirectory);
    }
    if (indexCacheDirectory == null) {
      if (helmHome == null) {
        helmHome = ChartRepository.getHelmHome();
        assert helmHome != null;
      }
      indexCacheDirectory = helmHome.resolve("repository/cache");
      assert indexCacheDirectory != null;
    }
    if (!Files.isDirectory(indexCacheDirectory)) {
      throw new IllegalArgumentException("!Files.isDirectory(indexCacheDirectory): " + indexCacheDirectory);
    }
    final Map<?, ?> map = new Yaml().loadAs(stream, Map.class);
    if (map == null || map.isEmpty()) {
      throw new IllegalArgumentException("No data readable from stream: " + stream);
    }
    final Set<ChartRepository> chartRepositories;
    @SuppressWarnings("unchecked")      
    final Collection<? extends Map<?, ?>> repositories = (Collection<? extends Map<?, ?>>)map.get("repositories");
    if (repositories == null || repositories.isEmpty()) {
      chartRepositories = Collections.emptySet();
    } else {
      chartRepositories = new LinkedHashSet<>();
      for (final Map<?, ?> repositoryMap : repositories) {
        if (repositoryMap != null && !repositoryMap.isEmpty()) {
          final String name = Objects.requireNonNull((String)repositoryMap.get("name"));
          final URI uri = new URI((String)repositoryMap.get("url"));
          Path cache = Objects.requireNonNull(Paths.get((String)repositoryMap.get("cache")));
          if (!cache.isAbsolute()) {
            cache = indexCacheDirectory.resolve(cache);
            assert cache.isAbsolute();
          }
          
          final ChartRepository chartRepository = new ChartRepository(name, uri, archiveCacheDirectory, indexCacheDirectory, cache);
          chartRepositories.add(chartRepository);
        }      
      }
    }
    return new ChartRepositoryRepository(chartRepositories);
  }
 
}
