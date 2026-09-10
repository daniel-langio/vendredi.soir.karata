import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

/// Only "dev" can create a listing right now (server-enforced - this is purely a client-side
/// convenience so most users don't see a "sell" affordance that would just 403 for them; opening
/// this up to more sellers later is a server config change, not something this needs to know).
const _sellerUsername = 'dev';

class MarketplaceScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;

  const MarketplaceScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
  });

  @override
  State<MarketplaceScreen> createState() => _MarketplaceScreenState();
}

class _MarketplaceScreenState extends State<MarketplaceScreen> {
  late final ApiClient _apiClient;
  List<Map<String, dynamic>> _listings = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final listings = await _apiClient.listMarketplaceListings();
      if (!mounted) return;
      setState(() => _listings = listings);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(AppLocalizations.of(context).couldNotLoadListings('$e')),
              backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openListing(Map<String, dynamic> listing) async {
    await Navigator.of(context).pushNamed('/marketplace/listing', arguments: {
      'serverUrl': widget.serverUrl,
      'token': widget.token,
      'username': widget.username,
      'listing': listing,
    });
    _load();
  }

  Future<void> _cancelListing(Map<String, dynamic> listing) async {
    final t = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.cancelListingTitle),
        content: Text(t.cancelListingContent),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: Text(t.cancel)),
          TextButton(onPressed: () => Navigator.pop(context, true), child: Text(t.cancelListingButton)),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await _apiClient.cancelListing(listing['id'] as String);
      _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text(t.couldNotCancelListing('$e')), backgroundColor: KarataColors.red),
        );
      }
    }
  }

  Future<void> _createListing() async {
    await Navigator.of(context).pushNamed('/marketplace/new', arguments: {
      'serverUrl': widget.serverUrl,
      'token': widget.token,
      'username': widget.username,
    });
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final canSell = widget.username == _sellerUsername;

    return Scaffold(
      appBar: AppBar(
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      floatingActionButton: canSell
          ? FloatingActionButton.extended(
              onPressed: _createListing,
              icon: const Icon(Icons.add),
              label: Text(t.newListing),
            )
          : null,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                t.marketplaceTitle,
                style: const TextStyle(
                    fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
              ),
              const SizedBox(height: 8),
              Text(
                t.marketplaceSubtitle,
                style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
              ),
              const SizedBox(height: 24),
              Expanded(
                child: _isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : _listings.isEmpty
                        ? Center(
                            child: Text(
                              t.noListingsYet,
                              textAlign: TextAlign.center,
                              style: const TextStyle(color: KarataColors.dim),
                            ),
                          )
                        : ListView.separated(
                            itemCount: _listings.length,
                            separatorBuilder: (context, index) =>
                                const Divider(height: 1, color: Color(0xFF1A181E)),
                            itemBuilder: (context, index) {
                              final listing = _listings[index];
                              final isMine = listing['sellerUsername'] == widget.username;
                              return ListTile(
                                contentPadding: EdgeInsets.zero,
                                leading: const Icon(Icons.monetization_on_rounded,
                                    color: KarataColors.chipInk),
                                title: Text(t.chipsAvailable('${listing['chipsAmount']}'),
                                    style:
                                        const TextStyle(color: KarataColors.ink, fontSize: 16.5)),
                                subtitle: Text(
                                  t.unitPriceLine('${listing['unitPriceAr']}', '${listing['provider']}'),
                                  style: const TextStyle(color: KarataColors.dim, fontSize: 12.5),
                                ),
                                trailing: isMine
                                    ? TextButton(
                                        onPressed: () => _cancelListing(listing),
                                        child: Text(t.cancelListingButton),
                                      )
                                    : TextButton(
                                        onPressed: () => _openListing(listing),
                                        child: Text(t.buy),
                                      ),
                                onTap: isMine ? null : () => _openListing(listing),
                              );
                            },
                          ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
