package vendredi.soir.karata.service;
import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;
@Service @AllArgsConstructor
public class DealService {
  private final GameService gs;
  public void takeAction(UUID did, ActionRequest req) {
    Game g = gs.getGameByDealId(did);
    Deal d = g.getCurrentDeal();
    Player p = g.getRules().determineNextPlayer(d, g.getPlayers());
    Action a = switch(req.actionType().toUpperCase()) {
      case "CHECK" -> new Check(p);
      case "CALL" -> new Call(p, req.amount());
      case "FOLD" -> new Fold(p);
      case "RAISE" -> new Raise(p, req.amount());
      case "BET" -> new Bet(p, req.amount());
      default -> throw new IllegalArgumentException("Unknown action type");
    };
    g.getDealer().execute(g, d, a);
    gs.saveAction(gs.getGameIdByDealId(did), did, a);
  }
  public void startDeal(UUID gid) {
    Game g = gs.getGame(gid);
    Deal d = g.startNewDeal(Deck.CLASSIC);
    UUID did = UUID.randomUUID();
    g.setCurrentDealId(did);
    Action s = new ShuffleDeck(d.getDeck().getCards());
    g.getDealer().execute(g, d, s);
    gs.saveAction(gid, did, s);
  }
  public vendredi.soir.karata.endpoint.rest.model.Hand getHand(UUID did, String user) {
    Game g = gs.getGameByDealId(did);
    return new vendredi.soir.karata.endpoint.rest.model.Hand(g.getCurrentDeal().getHoleCards(g.getPlayers().stream().filter(p -> p.getName().equals(user)).findFirst().orElseThrow()).stream().map(Object::toString).toList());
  }
}
