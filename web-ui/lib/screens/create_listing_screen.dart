import 'dart:async';
import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../l10n/app_localizations.dart';
import '../theme.dart';

class CreateListingScreen extends StatefulWidget {
  final String serverUrl;
  final String token;
  final String username;

  const CreateListingScreen({
    super.key,
    required this.serverUrl,
    required this.token,
    required this.username,
  });

  @override
  State<CreateListingScreen> createState() => _CreateListingScreenState();
}

class _CreateListingScreenState extends State<CreateListingScreen> {
  late final ApiClient _apiClient;
  final _chipsController = TextEditingController();
  final _priceController = TextEditingController();
  final _phoneController = TextEditingController();
  String _provider = 'MVOLA';
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: widget.serverUrl, token: widget.token);
    _loadDefaultPhoneNumber();
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
    _chipsController.dispose();
    _priceController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    final t = AppLocalizations.of(context);
    final chips = int.tryParse(_chipsController.text.trim());
    final price = int.tryParse(_priceController.text.trim());
    final phone = _phoneController.text.trim();

    if (chips == null || chips <= 0 || price == null || price <= 0 || phone.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(t.fillValidValues), backgroundColor: KarataColors.red),
      );
      return;
    }

    setState(() => _isLoading = true);
    try {
      await _apiClient.createListing(
        chipsAmount: chips,
        unitPriceAr: price,
        receivingPhoneNumber: phone,
        provider: _provider,
      );
      // Keeps the account's default phone number in sync with whatever was actually used here,
      // so the next listing (or purchase) starts prefilled with it - "per listing, but defaults
      // as if per-account".
      unawaited(_apiClient.setAccountPhoneNumber(phone));

      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(t.couldNotCreateListing('$e')), backgroundColor: KarataColors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
          children: [
            Text(
              t.newListing,
              style: const TextStyle(
                  fontSize: 34, fontWeight: FontWeight.w300, color: KarataColors.ink),
            ),
            const SizedBox(height: 8),
            Text(
              t.newListingSubtitle,
              style: const TextStyle(fontSize: 13.5, color: KarataColors.dim, height: 1.45),
            ),
            const SizedBox(height: 24),
            Text(t.chips,
                style: const TextStyle(
                    fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
            const SizedBox(height: 8),
            TextField(
              controller: _chipsController,
              keyboardType: TextInputType.number,
              style: const TextStyle(color: KarataColors.ink),
            ),
            const SizedBox(height: 20),
            Text(t.priceAr,
                style: const TextStyle(
                    fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
            const SizedBox(height: 8),
            TextField(
              controller: _priceController,
              keyboardType: TextInputType.number,
              style: const TextStyle(color: KarataColors.ink),
            ),
            const SizedBox(height: 20),
            Text(t.receivingPhoneNumber,
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
            Text(t.paymentProvider,
                style: const TextStyle(
                    fontSize: 14, fontWeight: FontWeight.w600, color: KarataColors.ink)),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              initialValue: _provider,
              dropdownColor: KarataColors.field,
              style: const TextStyle(color: KarataColors.ink),
              items: const [
                DropdownMenuItem(value: 'MVOLA', child: Text('MVola')),
                DropdownMenuItem(value: 'ORANGE_MONEY', child: Text('Orange Money')),
              ],
              onChanged: (value) => setState(() => _provider = value ?? _provider),
            ),
            const SizedBox(height: 28),
            ElevatedButton(
              onPressed: _isLoading ? null : _create,
              child: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2, color: KarataColors.ink),
                    )
                  : Text(t.createListing),
            ),
          ],
        ),
      ),
    );
  }
}
