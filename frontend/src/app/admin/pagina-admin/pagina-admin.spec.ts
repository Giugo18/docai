import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaAdmin } from './pagina-admin';

describe('PaginaAdmin', () => {
  let component: PaginaAdmin;
  let fixture: ComponentFixture<PaginaAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginaAdmin],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginaAdmin);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
