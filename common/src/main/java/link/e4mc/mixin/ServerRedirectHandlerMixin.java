package link.e4mc.mixin;

import link.e4mc.Config;
import link.e4mc.E4mcClient;
import link.e4mc.TicketSmuggler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Hashtable;
import java.util.Optional;

@Mixin(ServerRedirectHandler.class)
public interface ServerRedirectHandlerMixin {
    @Inject(method = "createDnsSrvRedirectHandler", at = @At("TAIL"), cancellable = true)
    private static void addDialtoneRedirectHandler(CallbackInfoReturnable<ServerRedirectHandler> cir) {
        DirContext dirContext;
        try {
            Class.forName("com.sun.jndi.dns.DnsContextFactory");
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            environment.put("java.naming.provider.url", "dns:");
            environment.put("com.sun.jndi.dns.timeout.retries", "1");
            dirContext = new InitialDirContext(environment);
            var innerHandler = cir.getReturnValue();
            cir.setReturnValue(serverAddress -> {
                var inner = innerHandler.lookupRedirect(serverAddress);
                if (!Config.INSTANCE.dialtonePlayerEnabled.value()) {
                    return inner;
                } if (inner.isPresent()) {
                    return inner;
                } else if (serverAddress.getPort() == 25565) {
                    try {
                        Attributes attributes = dirContext.getAttributes(serverAddress.getHost(), new String[]{"TXT"});
                        Attribute attribute = attributes.get("TXT");
                        if (attribute != null) {
                            var attrs = attribute.getAll();
                            while (attrs.hasMore()) {
                                var attr = attrs.next();
                                if (attr instanceof String str && str.startsWith("e4mc-dialtone-resolver=")) {
                                    var resolverAddr = str.substring(23);
                                    if (!serverAddress.getHost().endsWith(resolverAddr)) {
                                        E4mcClient.LOGGER.warn("Ignoring resolver addr {} as it's not a suffix of the target address", resolverAddr);
                                    }
                                    var httpClient = HttpClient.newHttpClient();
                                    var request = HttpRequest
                                            .newBuilder(new URI(String.format("https://" + resolverAddr + "/.well-known/dialtone_ticket/" + serverAddress.getHost())))
                                            .build();
                                    E4mcClient.LOGGER.info("req: {}", request);
                                    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                                    E4mcClient.LOGGER.info("resp: {}", response);
                                    if (response.statusCode() == 200 && response.body().startsWith("v1_")) {
                                        ((TicketSmuggler) (Object) serverAddress).e4mc$setSmuggledTicket(response.body().substring(3));
                                        return Optional.of(serverAddress);
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return Optional.empty();
            });
        } catch (Throwable e) {
            E4mcClient.LOGGER.warn("Failed to create a Dialtone redirect handler", e);
        }
    }
}
