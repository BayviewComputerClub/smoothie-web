/*
 * smoothie-web
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieContestService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class GlobalHandlerFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {

        SmoothieContestService contestService = SmoothieWebApplication.context.getBean(SmoothieContestService.class);
        SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);

        return request.principal()
                .flatMap(p -> {
                    // add currentContest that the user is in to request
                    if (p instanceof User) {
                        return userService.findUserById(((User) p).getId())
                                .flatMap(u -> {
                                    if (u.getContestId() != null) {
                                        return contestService.findContestById(u.getContestId());
                                    }
                                    return Mono.empty();
                                })
                                .doOnNext(c -> request.attributes().put("currentContest", c));
                    }
                    return Mono.empty();
                })
                .then(next.handle(request));
    }
}
