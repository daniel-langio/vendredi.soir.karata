import 'package:flutter/material.dart';

/// Hand-rolled localization: no ARB files or code generation, just a plain lookup table per
/// locale, registered through the real Localizations/LocalizationsDelegate APIs so it plugs into
/// MaterialApp exactly like a generated class would (AppLocalizations.of(context).xxx). Chosen
/// over `flutter gen-l10n` for a project this size - same result, no codegen step to maintain.
///
/// To add a language: add its Locale to [supportedLocales] and a full entry to [_strings] below
/// (copy the 'en' map as a starting point - any key missing from a non-English locale silently
/// falls back to the English string, so a partial translation never breaks the app).
class AppLocalizations {
  final Locale locale;
  const AppLocalizations(this.locale);

  static const supportedLocales = [Locale('en'), Locale('fr')];

  static const LocalizationsDelegate<AppLocalizations> delegate = _AppLocalizationsDelegate();

  static AppLocalizations of(BuildContext context) =>
      Localizations.of<AppLocalizations>(context, AppLocalizations)!;

  String _s(String key) =>
      _strings[locale.languageCode]?[key] ?? _strings['en']![key] ?? key;

  String _fmt(String key, Map<String, String> args) {
    var value = _s(key);
    args.forEach((k, v) => value = value.replaceAll('{$k}', v));
    return value;
  }

  // Common
  String get cancel => _s('cancel');
  String get logIn => _s('logIn');
  String get createAccount => _s('createAccount');
  String get required => _s('required');
  String get username => _s('username');
  String get password => _s('password');

  // WelcomeScreen
  String get welcomeTagline => _s('welcomeTagline');
  String get serverSettings => _s('serverSettings');
  String get hideServerSettings => _s('hideServerSettings');
  String get serverBaseUrl => _s('serverBaseUrl');

  // RegisterScreen
  String get registerSubtitle => _s('registerSubtitle');
  String get usernameTooShort => _s('usernameTooShort');
  String get passwordTooShort => _s('passwordTooShort');
  String couldNotCreateAccount(String error) => _fmt('couldNotCreateAccount', {'error': error});

  // LoginScreen
  String couldNotLogIn(String error) => _fmt('couldNotLogIn', {'error': error});

  // MenuScreen
  String get signInHint => _s('signInHint');
  String get createTable => _s('createTable');
  String get joinWithLink => _s('joinWithLink');
  String get yourTables => _s('yourTables');
  String get keptOnThisDevice => _s('keptOnThisDevice');
  String get noTablesYet => _s('noTablesYet');
  String get open => _s('open');
  String get logOut => _s('logOut');
  String get language => _s('language');
  String get systemDefault => _s('systemDefault');

  // NewTableScreen
  String get newTableTitle => _s('newTableTitle');
  String get newTableSubtitle => _s('newTableSubtitle');
  String get name => _s('name');
  String get smallBlind => _s('smallBlind');
  String get bigBlind => _s('bigBlind');
  String get yourBuyIn => _s('yourBuyIn');
  String get chips => _s('chips');
  String get newTableFooter => _s('newTableFooter');
  String get createAndSitDown => _s('createAndSitDown');
  String get fillValidValues => _s('fillValidValues');
  String couldNotCreateTable(String error) => _fmt('couldNotCreateTable', {'error': error});

  // JoinTableScreen
  String get joinTableTitle => _s('joinTableTitle');
  String get joinTableSubtitle => _s('joinTableSubtitle');
  String get linkHint => _s('linkHint');
  String get find => _s('find');
  String get foundIt => _s('foundIt');
  String blindsSeated(String small, String big, int count) =>
      _fmt('blindsSeated', {'small': small, 'big': big, 'count': '$count'});
  String get youCanOnlyBuyInOnce => _s('youCanOnlyBuyInOnce');
  String get sitDown => _s('sitDown');
  String get openTable => _s('openTable');
  String get noValidLink => _s('noValidLink');
  String tableNotFound(String error) => _fmt('tableNotFound', {'error': error});
  String get enterValidBuyIn => _s('enterValidBuyIn');
  String couldNotSitDown(String error) => _fmt('couldNotSitDown', {'error': error});

  // TableScreen
  String couldNotStartHand(String error) => _fmt('couldNotStartHand', {'error': error});
  String actionFailed(String action, String error) =>
      _fmt('actionFailed', {'action': action, 'error': error});
  String get inviteCopied => _s('inviteCopied');
  String get closeTableTitle => _s('closeTableTitle');
  String get closeTableContent => _s('closeTableContent');
  String get closeTable => _s('closeTable');
  String get closedSuffix => _s('closedSuffix');
  String couldNotCloseTable(String error) => _fmt('couldNotCloseTable', {'error': error});
  String get leaveTableTitle => _s('leaveTableTitle');
  String get leaveTableContent => _s('leaveTableContent');
  String get leaveTable => _s('leaveTable');
  String couldNotLeaveTable(String error) => _fmt('couldNotLeaveTable', {'error': error});
  String get copyInvite => _s('copyInvite');
  String get live => _s('live');
  String get reconnecting => _s('reconnecting');
  String lastUpdateAgo(int secs) => _fmt('lastUpdateAgo', {'secs': '$secs'});
  String yourTurnLeft(int secs) => _fmt('yourTurnLeft', {'secs': '$secs'});
  String waitingOn(String username) => _fmt('waitingOn', {'username': username});
  String get tableClosed => _s('tableClosed');
  String get startTheHand => _s('startTheHand');
  String get nextHand => _s('nextHand');
  String get fold => _s('fold');
  String get check => _s('check');
  String call(int amount) => _fmt('call', {'amount': '$amount'});
  String bet(int amount) => _fmt('bet', {'amount': '$amount'});
  String raise(int amount) => _fmt('raise', {'amount': '$amount'});
  String minAllIn(int min, int max) => _fmt('minAllIn', {'min': '$min', 'max': '$max'});
  String get min => _s('min');
  String get halfPot => _s('halfPot');
  String get pot => _s('pot');
  String get allIn => _s('allIn');
  String get handStrengthAvailableSoon => _s('handStrengthAvailableSoon');
  String won(String names, int amount) => _fmt('won', {'names': names, 'amount': '$amount'});
  String get sb => _s('sb');
  String get bb => _s('bb');
  String get allInTag => _s('allInTag');

  static const Map<String, Map<String, String>> _strings = {
    'en': {
      'cancel': 'Cancel',
      'logIn': 'Log in',
      'createAccount': 'Create account',
      'required': 'Required',
      'username': 'Username',
      'password': 'Password',
      'welcomeTagline':
          'Play poker with your friends. No accounts to manage,\njust a name and a table.',
      'serverSettings': 'Server settings',
      'hideServerSettings': 'Hide server settings',
      'serverBaseUrl': 'Server base URL',
      'registerSubtitle': 'This name is how everyone else at the table will see you.',
      'usernameTooShort': 'At least 3 characters',
      'passwordTooShort': 'At least 6 characters',
      'couldNotCreateAccount': 'Could not create account: {error}',
      'couldNotLogIn': 'Could not log in: {error}',
      'signInHint': 'This name is your sign-in',
      'createTable': 'Create a table',
      'joinWithLink': 'Join with a link',
      'yourTables': 'Your tables',
      'keptOnThisDevice': 'kept on this device',
      'noTablesYet': 'No tables yet. Create or join one to see it here.',
      'open': 'Open',
      'logOut': 'Log out',
      'language': 'Language',
      'systemDefault': 'System default',
      'newTableTitle': 'New table',
      'newTableSubtitle':
          'Name and blinds are all the server keeps. Everything else is set when each player sits down.',
      'name': 'Name',
      'smallBlind': 'Small blind',
      'bigBlind': 'Big blind',
      'yourBuyIn': 'Your buy-in',
      'chips': 'Chips',
      'newTableFooter':
          "Creating the table seats you at it. Everyone else picks their own buy-in when they join, and chips carry between hands.",
      'createAndSitDown': 'Create and sit down',
      'fillValidValues': 'Please fill in valid values',
      'couldNotCreateTable': 'Could not create table: {error}',
      'joinTableTitle': 'Join a table',
      'joinTableSubtitle':
          "Paste the link someone sent you. Table IDs are long, so nobody should ever have to read one out.",
      'linkHint': 'karata.app/t/…',
      'find': 'Find',
      'foundIt': 'Found it',
      'blindsSeated': 'Blinds {small} / {big} · {count} seated',
      'youCanOnlyBuyInOnce':
          "You can only buy in once per table, so pick a stack you're happy to sit with.",
      'sitDown': 'Sit down',
      'openTable': 'Open table',
      'noValidLink': 'No valid table link found',
      'tableNotFound': 'Table not found: {error}',
      'enterValidBuyIn': 'Enter a valid buy-in',
      'couldNotSitDown': 'Could not sit down: {error}',
      'couldNotStartHand': 'Could not start hand: {error}',
      'actionFailed': '{action} failed: {error}',
      'inviteCopied': 'Invite copied — send it to whoever you want to invite',
      'closeTableTitle': 'Close this table?',
      'closeTableContent':
          'Nobody will be able to join, start a hand, or act at this table again. This cannot be undone.',
      'closeTable': 'Close table',
      'closedSuffix': 'closed',
      'couldNotCloseTable': 'Could not close table: {error}',
      'leaveTableTitle': 'Leave this table?',
      'leaveTableContent':
          "You'll be folded out of the current hand if you're still in it, and won't be dealt into any future ones here. You can't sit back down afterwards.",
      'leaveTable': 'Leave table',
      'couldNotLeaveTable': 'Could not leave table: {error}',
      'copyInvite': 'Copy invite',
      'live': 'Live',
      'reconnecting': 'Reconnecting',
      'lastUpdateAgo': 'Last update {secs}s ago',
      'yourTurnLeft': 'Your turn — {secs}s left',
      'waitingOn': 'Waiting on {username}',
      'tableClosed': 'THIS TABLE IS CLOSED',
      'startTheHand': 'Start the hand',
      'nextHand': 'Next hand',
      'fold': 'Fold',
      'check': 'Check',
      'call': 'Call {amount}',
      'bet': 'Bet {amount}',
      'raise': 'Raise {amount}',
      'minAllIn': 'min {min} · all in {max}',
      'min': 'Min',
      'halfPot': '½ pot',
      'pot': 'Pot',
      'allIn': 'All in',
      'handStrengthAvailableSoon': 'Hand strength\navailable soon',
      'won': '💰 {names} won {amount}',
      'sb': 'SB',
      'bb': 'BB',
      'allInTag': 'ALL',
    },
    'fr': {
      'cancel': 'Annuler',
      'logIn': 'Se connecter',
      'createAccount': 'Créer un compte',
      'required': 'Requis',
      'username': "Nom d'utilisateur",
      'password': 'Mot de passe',
      'welcomeTagline':
          'Jouez au poker avec vos amis. Pas de compte à gérer,\njuste un nom et une table.',
      'serverSettings': 'Paramètres du serveur',
      'hideServerSettings': 'Masquer les paramètres du serveur',
      'serverBaseUrl': 'URL de base du serveur',
      'registerSubtitle': 'Ce nom est celui que les autres joueurs verront à la table.',
      'usernameTooShort': 'Au moins 3 caractères',
      'passwordTooShort': 'Au moins 6 caractères',
      'couldNotCreateAccount': 'Impossible de créer le compte : {error}',
      'couldNotLogIn': 'Impossible de se connecter : {error}',
      'signInHint': 'Ce nom est votre identifiant de connexion',
      'createTable': 'Créer une table',
      'joinWithLink': 'Rejoindre avec un lien',
      'yourTables': 'Vos tables',
      'keptOnThisDevice': 'conservées sur cet appareil',
      'noTablesYet': 'Aucune table pour le moment. Créez-en une ou rejoignez-en une pour la voir ici.',
      'open': 'Ouvrir',
      'logOut': 'Se déconnecter',
      'language': 'Langue',
      'systemDefault': 'Système',
      'newTableTitle': 'Nouvelle table',
      'newTableSubtitle':
          'Le serveur ne conserve que le nom et les blindes. Tout le reste est défini quand chaque joueur s\'installe.',
      'name': 'Nom',
      'smallBlind': 'Petite blinde',
      'bigBlind': 'Grosse blinde',
      'yourBuyIn': "Votre mise d'entrée",
      'chips': 'Jetons',
      'newTableFooter':
          "Créer la table vous y installe directement. Chacun choisit sa propre mise d'entrée en rejoignant, et les jetons sont conservés d'une main à l'autre.",
      'createAndSitDown': "Créer et s'installer",
      'fillValidValues': 'Veuillez saisir des valeurs valides',
      'couldNotCreateTable': 'Impossible de créer la table : {error}',
      'joinTableTitle': 'Rejoindre une table',
      'joinTableSubtitle':
          "Collez le lien que quelqu'un vous a envoyé. Les identifiants de table sont longs, personne ne devrait avoir à en lire un à voix haute.",
      'linkHint': 'karata.app/t/…',
      'find': 'Rechercher',
      'foundIt': 'Trouvée',
      'blindsSeated': 'Blindes {small} / {big} · {count} joueurs',
      'youCanOnlyBuyInOnce':
          "Vous ne pouvez acheter des jetons qu'une seule fois par table : choisissez une pile qui vous convient.",
      'sitDown': "S'installer",
      'openTable': 'Ouvrir la table',
      'noValidLink': 'Aucun lien de table valide trouvé',
      'tableNotFound': 'Table introuvable : {error}',
      'enterValidBuyIn': "Saisissez une mise d'entrée valide",
      'couldNotSitDown': "Impossible de s'installer : {error}",
      'couldNotStartHand': 'Impossible de démarrer la main : {error}',
      'actionFailed': '{action} a échoué : {error}',
      'inviteCopied': "Invitation copiée — envoyez-la à qui vous voulez inviter",
      'closeTableTitle': 'Fermer cette table ?',
      'closeTableContent':
          'Plus personne ne pourra rejoindre, démarrer une main ni jouer à cette table. Cette action est irréversible.',
      'closeTable': 'Fermer la table',
      'closedSuffix': 'fermée',
      'couldNotCloseTable': 'Impossible de fermer la table : {error}',
      'leaveTableTitle': 'Quitter cette table ?',
      'leaveTableContent':
          "Vous serez couché de la main en cours si vous y êtes encore, et vous ne serez plus distribué dans les mains suivantes ici. Vous ne pourrez pas vous rasseoir ensuite.",
      'leaveTable': 'Quitter la table',
      'couldNotLeaveTable': 'Impossible de quitter la table : {error}',
      'copyInvite': "Copier l'invitation",
      'live': 'En direct',
      'reconnecting': 'Reconnexion',
      'lastUpdateAgo': 'Dernière mise à jour il y a {secs} s',
      'yourTurnLeft': 'À vous de jouer — {secs} s restantes',
      'waitingOn': 'En attente de {username}',
      'tableClosed': 'CETTE TABLE EST FERMÉE',
      'startTheHand': 'Démarrer la main',
      'nextHand': 'Main suivante',
      'fold': 'Se coucher',
      'check': 'Check',
      'call': 'Suivre {amount}',
      'bet': 'Miser {amount}',
      'raise': 'Relancer {amount}',
      'minAllIn': 'min {min} · tapis {max}',
      'min': 'Min',
      'halfPot': '½ pot',
      'pot': 'Pot',
      'allIn': 'Tapis',
      'handStrengthAvailableSoon': 'Force de la main\nbientôt disponible',
      'won': '💰 {names} remporte {amount}',
      'sb': 'PB',
      'bb': 'GB',
      'allInTag': 'TAPIS',
    },
  };
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) =>
      AppLocalizations.supportedLocales.any((l) => l.languageCode == locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async => AppLocalizations(locale);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
