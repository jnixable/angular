import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-account-overview',
  imports: [],
  templateUrl: './account-overview.html',
  styleUrl: './account-overview.scss',
})
export class AccountOverviewPage {
  private readonly route = inject(ActivatedRoute);
  protected readonly accountId = this.route.snapshot.paramMap.get('accountId');
}
