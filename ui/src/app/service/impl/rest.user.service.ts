import {Injectable} from "@angular/core";
import {Observable} from "rxjs/Observable";
import "rxjs/add/observable/of";
import {HttpClient} from "@angular/common/http";
import {UserService} from "../user.service";
import {User} from "../../model/User";
import {environment} from "../../../environments/environment";

@Injectable()
export class RestUserService implements UserService {
  constructor(private http: HttpClient) {}

  whoami(): Observable<User> {
    return this.http.get<User>(environment.api.baseurl + "whoami")
  }

  users(query: string, page: number): Observable<User[]> {
    return this.http.get<User[]>(environment.api.baseurl + "users", {
      params: {
        q: query,
        page: page.toString(),
      }
    });
  }

  user(username: string): Observable<User> {
    // TODO replace this with http call to /user endpoint when it has been created
    let user = new User();
    user.username = username;
    return Observable.of(user);
  }
}