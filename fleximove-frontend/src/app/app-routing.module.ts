import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { AccountTypeSelectorComponent } from './auth/register/account-type-selector.component';
import { RegisterPrivateComponent } from './auth/register/register-private/register-private.component';
import { RegisterBusinessComponent } from './auth/register/register-business/register-business.component';
import { CustomerLayoutComponent } from './layout/customer/customer-layout/customer-layout.component';
import { CustomerHomepageComponent } from './customer-pages/customer-homepage/customer-homepage.component';
import { VehicleSearchResultComponent } from './customer-pages/vehicle-search-result/vehicle-search-result.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'register',
    children: [
      { path: '', component: AccountTypeSelectorComponent },
      { path: 'private', component: RegisterPrivateComponent },
      { path: 'business', component: RegisterBusinessComponent }
    ]
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {path: 'customer', component: CustomerLayoutComponent,
    children: [
      { path: '', component: CustomerHomepageComponent },
      { path: 'home', component: CustomerHomepageComponent },
      { path: 'search-results', component: VehicleSearchResultComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
