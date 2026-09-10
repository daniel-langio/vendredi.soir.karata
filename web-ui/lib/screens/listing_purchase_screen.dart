import 'dart:async';
import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

class ListingPurchaseScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;
  final Map<String, dynamic> listing;

  const ListingPurchaseScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
    required this.listing,
  });

  @override
  State<ListingPurchaseScreen> createState() => _ListingPurchaseScreenState();
}

class _ListingPurchaseScreenState extends State<ListingPurchaseScreen> {
  late final ApiClient _apiClient;
  late Map<String, dynamic> _listing;
  final _phoneController = TextEditingController();
  final _refController = TextEditingController();
  bool _isLoading = false;
  Timer? _pollTimer;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
    _listing = widget.listing;
    _loadDefaultPhoneNumber();
    if (_listing['status'] == 'PENDING_PAYMENT') _startPolling();
  }

  Future<void> _loadDefaultPhoneNumber() async {
    try {
      final phone = await _apiClient.getAccountPhoneNumber();
      if (mounted && phone != null) setState(() => _phoneController.text = phone);
    } catch (_) {
      // Non-critical - the field just starts empty if we can't reach the server.
    }
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    _phoneController.dispose();
    _refController.dispose();
    super.dispose();
  }

  void _startPolling() {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(const Duration(seconds: 4), (_) async {
      try {
        final updated = await _apiClient.getListing(_listing['id'] as String);
        if (!mounted) return;
        setState(() => _listing = updated);
        if (updated['status'] != 'PENDING_PAYMENT') _pollTimer?.cancel();
      } catch (_) {
        // Transient network hiccup while polling - just try again next tick.
      }
    });
  }

  Future<void> _submitPayment() async {
    final t = AppLocalizations.of(context);
    final phone = _phoneController.text.trim();
    final ref = _refController.text.trim();
    if (phone.isEmpty || ref.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(t.fillValidValues), backgroundColor: KarataColors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      final updated = await _apiClient.buyListing(
        id: _listing['id'] as String,
        buyerPhoneNumber: phone,
        pspRef: ref,
      );
      if (!mounted) return;
      setState(() => _listing = updated);
      _startPolling();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(t.couldNotBuyListing('$e')), backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final status = _listing['status'] as String;

    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            Text(
              '${_listing['chipsAmount']} ${t.chips}',
              style: const TextStyle(
                  fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            Text(
              '${_listing['priceAr']} Ar · ${_listing['provider']}',
              style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            if (status == 'ACTIVE') ...[
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: KarataColors.field,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Text(
                  t.payInstructions(
                      '${_listing['priceAr']}', '${_listing['receivingPhoneNumber']}'),
                  style: const TextStyle(color: KarataColors.ink, fontSize: 13.5, height: 1.5),
                ),
              ),
              const SizedBox(height: 24),
              Text(t.yourPhoneNumber,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
              const SizedBox(height: 8),
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                style: const TextStyle(color: KarataColors.ink),
                decoration: const InputDecoration(hintText: '+261...'),
              ),
              const SizedBox(height: 20),
              Text(t.transactionRef,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
              const SizedBox(height: 8),
              TextField(
                controller: _refController,
                style: const TextStyle(color: KarataColors.ink),
                decoration: InputDecoration(hintText: t.transactionRefHint),
              ),
              const SizedBox(height: 28),
              ElevatedButton(
                onPressed: _isLoading ? null : _submitPayment,
                child: _isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: KarataColors.ink),
                      )
                    : Text(t.submitPayment),
              ),
            ] else if (status == 'PENDING_PAYMENT') ...[
              const SizedBox(height: 40),
              const Center(child: CircularProgressIndicator()),
              const SizedBox(height: 20),
              Text(
                t.waitingForConfirmation,
                textAlign: TextAlign.center,
                style: const TextStyle(color: KarataColors.dim, fontSize: 13.5, height: 1.5),
              ),
            ] else if (status == 'SOLD') ...[
              const SizedBox(height: 40),
              const Center(
                  child: Icon(Icons.check_circle, color: KarataColors.chipInk, size: 56)),
              const SizedBox(height: 20),
              Text(
                t.chipsCredited,
                textAlign: TextAlign.center,
                style: const TextStyle(color: KarataColors.ink, fontSize: 16),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                child: Text(t.close),
              ),
            ] else ...[
              const SizedBox(height: 40),
              Text(
                t.listingNoLongerAvailable,
                textAlign: TextAlign.center,
                style: const TextStyle(color: KarataColors.dim, fontSize: 13.5),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
