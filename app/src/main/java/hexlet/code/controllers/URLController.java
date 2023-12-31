package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;

import io.ebean.PagedList;

import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class URLController {
    private static final String INPUT_URL_NULL = "inputUrl can't be null";

    public static Handler createUrl = ctx -> {
        String inputUrl = ctx.formParam("url");
        URL parsedUrl;

        try {
            parsedUrl = new URI(Objects.requireNonNull(inputUrl, INPUT_URL_NULL)).toURL();
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        String normalizedUrl = transformUrl(parsedUrl);
        Url checkedUrl = new QUrl()
                .name.equalTo(normalizedUrl)
                .findOne();

        if (checkedUrl != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
            ctx.redirect("/");
            return;
        }
        Url url = new Url(normalizedUrl);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");

        ctx.redirect("/urls");
    };

    public static Handler showAllAddedUrls = ctx -> {
        String term = ctx.queryParamAsClass("term", String.class).getOrDefault("");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .name.icontains(term)
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();
        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());
        ctx.attribute("term", term);
        ctx.attribute("urls", urls);
        ctx.attribute("currentPage", currentPage);
        ctx.attribute("pages", pages);

        ctx.render("urls/index.html");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", long.class).getOrDefault(null);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse("Url with id - " + id + " is not found in database!");
        }
        ctx.attribute("url", url);

        ctx.render("urls/url.html");
    };

    public static Handler addCheck = ctx -> {
        long id = ctx.pathParamAsClass("id", long.class).getOrDefault(null);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse("Url with id - " + id + " is not found in database!");
        }

        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();
            Document parsedPage = Jsoup.parse(response.getBody());

            int statusCode = response.getStatus();
            String title = parsedPage.title();
            String h1 = parsedPage.selectFirst("h1") == null
                    ? null : parsedPage.selectFirst("h1").text();
            String description = parsedPage.selectFirst("meta[name=description]") == null
                    ? null : parsedPage.selectFirst("meta[name=description]").attr("content");

            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Проблемы с доступом к сайту, попробуйте в другой раз!");
            ctx.sessionAttribute("flash-type", "danger");
        } catch (Exception e) {
            ctx.sessionAttribute("flash", e.getMessage());
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + id);
    };

    private static String transformUrl(URL parsedUrl) {
        String protocol = parsedUrl.getProtocol();
        String authority = parsedUrl.getAuthority();
        int port = parsedUrl.getPort();

        return String.format("%s://%s",
                        protocol,
                        authority);
    }
}
