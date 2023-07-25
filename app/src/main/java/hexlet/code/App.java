package hexlet.code;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlsController;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.get;

public final class App {

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "3000");
        return Integer.valueOf(port);
    }
    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
                    config.plugins.enableDevLogging();
                    JavalinThymeleaf.init(getTemplateEngine()); })
                .get("/", ctx -> ctx.render("index.html"))
                .get("/url/", see)
                .post("/url/", create);
        return app;
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setCharacterEncoding("UTF-8");

        templateEngine.addTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "8080");
        return Integer.parseInt(port);
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.mainPage);

        app.routes(() -> {
            path("urls", () -> {
                post(UrlsController.createUrl);
                get(UrlsController.showAllAddedUrls);
                path("{id}", () -> {
                    get(UrlsController.showUrl);
                    path("checks", () -> post(UrlsController.addCheck));
                });
            });
        });
    }
}